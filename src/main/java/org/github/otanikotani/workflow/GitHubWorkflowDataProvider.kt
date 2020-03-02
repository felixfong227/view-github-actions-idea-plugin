package org.github.otanikotani.workflow

import org.jetbrains.annotations.CalledInAwt

interface GitHubWorkflowDataProvider {
    val number: Long

    @CalledInAwt
    fun reloadDetails()

    @CalledInAwt
    fun reloadChanges()

    @CalledInAwt
    fun reloadReviewThreads()

}