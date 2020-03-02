package org.github.otanikotani.workflow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.messages.Topic
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.github.api.GithubApiRequestExecutorManager
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager
import org.jetbrains.plugins.github.authentication.accounts.AccountTokenChangedListener
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.authentication.ui.GithubChooseAccountDialog
import org.jetbrains.plugins.github.pullrequest.GHPRRequestExecutorComponent
import org.jetbrains.plugins.github.ui.util.DisposingWrapper
import org.jetbrains.plugins.github.util.GitRemoteUrlCoordinates
import org.jetbrains.plugins.github.util.GithubUIUtil

internal class GitHubWorkflowAccountsComponent(private val authManager: GithubAuthenticationManager,
                                               private val project: Project,
                                               private val remoteUrl: GitRemoteUrlCoordinates,
                                               parentDisposable: Disposable)
    : DisposingWrapper(parentDisposable) {

    private val requestExecutorManager by lazy(LazyThreadSafetyMode.NONE) { GithubApiRequestExecutorManager.getInstance() }
    private var selectedAccount: GithubAccount? = null

    init {
        background = UIUtil.getListBackground()

        ApplicationManager.getApplication().messageBus.connect(parentDisposable)
            .subscribe(Topic.create("GITHUB_ACCOUNT_TOKEN_CHANGED", AccountTokenChangedListener::class.java),
                object : AccountTokenChangedListener {
                    override fun tokenChanged(account: GithubAccount) {
                        update()
                    }
                })
        //TODO: listen to default account changes?
        update()
    }

    private fun update() {
        if (selectedAccount != null) return

        val accounts = authManager.getAccounts().filter { it.server.matches(remoteUrl.url) }

        if (accounts.size == 1) {
            setActualContent(accounts.single())
            return
        }

        val defaultAccount = accounts.find { it == authManager.getDefaultAccount(project) }
        if (defaultAccount != null) {
            setActualContent(defaultAccount)
            return
        }

        if (accounts.isNotEmpty()) {
            showChooseAccountPanel(accounts)
        } else {
            showLoginPanel()
        }
    }

    private fun showLoginPanel() {
        setCenteredContent(GithubUIUtil.createNoteWithAction(::requestNewAccount).apply {
            append("Log in", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, Runnable { requestNewAccount() })
            append(" to GitHub to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        })
    }

    private fun requestNewAccount() {
        authManager.requestNewAccount(project)
        IdeFocusManager.getInstance(project).requestFocusInProject(this@GitHubWorkflowAccountsComponent, project)
    }

    private fun showChooseAccountPanel(accounts: List<GithubAccount>) {
        setCenteredContent(GithubUIUtil.createNoteWithAction { chooseAccount(accounts) }.apply {
            append("Select", SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES, Runnable { chooseAccount(accounts) })
            append(" GitHub account to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        })
    }

    private fun chooseAccount(accounts: List<GithubAccount>) {
        val dialog = GithubChooseAccountDialog(project, null, accounts, null, true, true)
        if (dialog.showAndGet()) {
            setActualContent(dialog.account)
            IdeFocusManager.getInstance(project).requestFocusInProject(this@GitHubWorkflowAccountsComponent, project)
        }
    }

    private fun setActualContent(account: GithubAccount) {
        selectedAccount = account
        val disposable = Disposer.newDisposable()
        setContent(GHPRRequestExecutorComponent(requestExecutorManager, project, remoteUrl, account, disposable), disposable)
    }
}