// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.space.vcs.review.list

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.space.messages.SpaceBundle
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

internal class SpaceReviewListFiltersPanel(private val listVm: SpaceReviewsListVm) {

  private val searchTextField = object : SearchTextField("space.review.list.search.text") {

    override fun processKeyBinding(ks: KeyStroke?, e: KeyEvent?, condition: Int, pressed: Boolean): Boolean {
      if (e?.keyCode == KeyEvent.VK_ENTER && pressed) {
        onTextChanged()
        return true
      }
      return super.processKeyBinding(ks, e, condition, pressed)
    }

    override fun onFocusLost() {
      super.onFocusLost()
      onTextChanged()
    }

    override fun onFieldCleared() {
      onTextChanged()
    }

    private fun onTextChanged() {
      listVm.textToSearch.value = text.trim()
    }
  }

  private val quickFiltersComboBox = ComboBox(EnumComboBoxModel(ReviewListQuickFilter::class.java)).apply {
    addActionListener {
      val stateFilter = this.selectedItem as ReviewListQuickFilter
      listVm.spaceReviewsQuickFilter.value = listVm.quickFiltersMap.value[stateFilter] ?: error(
        "Unable to resolve quick filter settings for ${stateFilter}")
    }

    selectedItem = DEFAULT_QUICK_FILTER
  }

  val view = NonOpaquePanel(BorderLayout())

  init {
    val quickFiltersPanel = NonOpaquePanel(BorderLayout()).apply {
      add(JBLabel(SpaceBundle.message("label.quick.filters")).withBorder(JBUI.Borders.empty(0, 5)), BorderLayout.WEST)
      add(quickFiltersComboBox, BorderLayout.CENTER)
      add(createRefreshButton(), BorderLayout.EAST)
    }

    view.add(searchTextField, BorderLayout.NORTH)
    view.add(quickFiltersPanel, BorderLayout.CENTER)
  }

  private fun createRefreshButton(): JComponent {
    val refreshAction = object : DumbAwareAction(SpaceBundle.messagePointer("action.refresh.reviews.text"), AllIcons.Actions.Refresh) {
      override fun actionPerformed(e: AnActionEvent) {
        listVm.refresh()
      }
    }
    return ActionButton(refreshAction, refreshAction.templatePresentation, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  }
}