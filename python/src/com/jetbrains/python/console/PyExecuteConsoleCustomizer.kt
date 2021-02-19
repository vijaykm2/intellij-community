// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.console

import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

interface PyExecuteConsoleCustomizer {
  companion object {
    private val EP_NAME: ExtensionPointName<PyExecuteConsoleCustomizer> =
      ExtensionPointName.create("com.jetbrains.python.console.executeCustomizer")

    val instance: PyExecuteConsoleCustomizer
      get() = EP_NAME.extensionList.first()
  }

  fun isCustomDescriptorSupported(virtualFile: VirtualFile): Boolean = false

  fun getCustomDescriptorType(virtualFile: VirtualFile): DescriptorType? = null

  fun getExistingDescriptor(virtualFile: VirtualFile): RunContentDescriptor? = null

  fun updateDescriptor(virtualFile: VirtualFile, type: DescriptorType, descriptor: RunContentDescriptor?) {}
}

enum class DescriptorType {
  NEW, EXISTING
}