/*
 * Copyright 2019 Pandora Media, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * See accompanying LICENSE file or you may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pandora.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.actions.VcsContextFactory
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.vcsUtil.VcsUtil
import java.io.IOException

val logger = Logger.getInstance("Kotlin Converter")

/**
 * Official identifier of the native `ConvertJavaToKotlin` action.
 */
const val CONVERT_JAVA_TO_KOTLIN_PLUGIN_ID = "ConvertJavaToKotlin"

const val SUGGESTED_COMMIT_MESSAGE = "Converting files to Kotlin with safe renaming."

const val KOTLIN_EXTENSION = "kt"

const val JAVA_EXTENSION = "java"

private var lastCommitMessage = SUGGESTED_COMMIT_MESSAGE

fun AnAction.writeCommitHistory(project: Project, projectBase: VirtualFile, files: Array<VirtualFile>): Boolean {

    val commitMessage = Messages.showInputDialog(project, "Commit Message for Conversion:", "Enter a commit message", null, lastCommitMessage, null)
    if (commitMessage.isNullOrBlank()) {
        return false
    }
    lastCommitMessage = commitMessage!!

    val finalVcs = VcsUtil.getVcsFor(project, projectBase) ?: return false
    val changes = files.mapNotNull {
        logger.info("File $it has extension: ${it.extension}")
        if (it.extension != JAVA_EXTENSION) return@mapNotNull null
        val before = it.contentRevision()
        logger.info("Found file ${before.file}")
        renameFile(project, it, "${it.nameWithoutExtension}.$KOTLIN_EXTENSION")
        val after = it.contentRevision()
        logger.info("Renamed file ${before.file} -> ${after.file}")
        Change(before, after)
    }.toList()
    if (changes.isNotEmpty()) {
        finalVcs.checkinEnvironment?.commit(changes, commitMessage)
    } else {
        Messages.showDialog("No files found to commit.", "Nothing to commit", emptyArray(), 0, null)
        logger.info("Cannot commit an empty set of files.")
        return false
    }
    files.forEach {
        if (it.extension != KOTLIN_EXTENSION) return@forEach
        renameFile(project, it, "${it.nameWithoutExtension}.$JAVA_EXTENSION")
    }
    return true
}

fun AnAction.renameFile(project: Project, virtualFile: VirtualFile, newName: String) {

    logger.info("Renaming file `${virtualFile.name}` to `$newName`")

    WriteCommandAction.runWriteCommandAction(project) {
        try {
            virtualFile.rename(this, newName)
        } catch (e: IOException) {
            throw RuntimeException("Error while renaming file `${virtualFile.name}` to `$newName`", e)
        }
    }
}

fun VirtualFile.contentRevision(): CurrentContentRevision {
    val contextFactory = VcsContextFactory.SERVICE.getInstance()
    val path = contextFactory.createFilePathOn(this)
    return CurrentContentRevision(path)
}

fun AnActionEvent.anyJavaFileSelected(): Boolean {
    val project = project ?: return false
    val files = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return false
    return anyJavaFileSelected(project, files)
}

private fun anyJavaFileSelected(project: Project, files: Array<out VirtualFile>): Boolean {
    val manager = PsiManager.getInstance(project)
    if (files.any { manager.findFile(it) is PsiJavaFile && it.isWritable }) return true
    return files.any { it.isDirectory && anyJavaFileSelected(project, it.children) }
}

