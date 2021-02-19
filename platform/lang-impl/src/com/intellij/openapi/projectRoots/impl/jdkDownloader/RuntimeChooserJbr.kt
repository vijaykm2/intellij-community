// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.lang.LangBundle
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Path
import java.nio.file.Paths

data class RuntimeChooserDownloadableItem(val item: JdkItem) : RuntimeChooserItem() {
  override fun toString() = item.fullPresentationText
}

fun RuntimeChooserModel.fetchAvailableJbrs() {
  object : Task.Backgroundable(null, LangBundle.message("progress.title.choose.ide.runtime.downloading.jetbrains.runtime.list")) {
    override fun run(indicator: ProgressIndicator) {
      val builds = service<RuntimeChooserJbrListDownloader>().downloadForUI(indicator)
      invokeLater(modalityState = ModalityState.any()) {
        updateDownloadJbrList(builds)
      }
    }
  }.queue()
}

@Service(Service.Level.APP)
private class RuntimeChooserJbrListDownloader : JdkListDownloaderBase() {
  override val feedUrl: String by lazy {
    val majorVersion = ApplicationInfo.getInstance().build.components.firstOrNull()
    "https://download.jetbrains.com/jdk/feed/v1/jbr-choose-runtime-${majorVersion}.json.xz"
  }
}

