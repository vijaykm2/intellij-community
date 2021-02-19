// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.updateSettings.impl

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.WhatsNewAction
import com.intellij.ide.plugins.newui.PluginLogo
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MultiLineLabel
import com.intellij.openapi.updateSettings.UpdateStrategyCustomization
import com.intellij.openapi.util.NlsContexts.Label
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.*
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val TOOLBOX_URL = "https://www.jetbrains.com/toolbox-app/?utm_source=product&utm_medium=link&utm_campaign=toolbox_app_in_IDE_updatewindow&utm_content=we_recommend"

class UpdateSettingsConfigurable @JvmOverloads constructor (private val checkNowEnabled: Boolean = true) :
  BoundConfigurable(IdeBundle.message("updates.settings.title"), "preferences.updates") {

  private var myLink: JComponent? = null
  private var myLastCheckedLabel: JLabel? = null

  override fun createPanel(): DialogPanel {
    val settings = UpdateSettings.getInstance()
    val manager = ExternalUpdateManager.ACTUAL
    val eapLocked = ApplicationInfoEx.getInstanceEx().isMajorEAP && UpdateStrategyCustomization.getInstance().forceEapUpdateChannelForEapBuilds()
    val appInfo = ApplicationInfo.getInstance()
    val channelModel = CollectionComboBoxModel(settings.activeChannels)

    return panel {
      val productText = IdeBundle.message("updates.settings.current.version") + ' ' + ApplicationNamesInfo.getInstance().fullProductName +
                        ' ' + appInfo.fullVersion
      row(productText) {
        contextLabel(appInfo.build.asString() + ' ' + DateFormatUtil.formatAboutDialogDate(appInfo.buildDate.time))

        if (manager == null) {
          largeGapAfter()
        }
      }

      if (manager == null) {
        row {
          cell {
            val checkBox = checkBox(IdeBundle.message(if (eapLocked) "updates.settings.checkbox" else "updates.settings.checkbox.for"),
                                    settings.state::isCheckNeeded)
            if (eapLocked) {
              contextLabel(IdeBundle.message("updates.settings.channel.locked")).withLargeLeftGap()
            }
            else {
              comboBox(channelModel, getter = { settings.selectedActiveChannel },
                       setter = { settings.selectedChannelStatus = selectedChannel(it) }).enableIf(checkBox.selected)
            }
          }
        }
      }
      else {
        row {
          val text = when (manager) {
            ExternalUpdateManager.TOOLBOX -> IdeBundle.message("updates.settings.external", "Toolbox")
            else -> IdeBundle.message("updates.settings.external", manager.toolName)
          }
          contextLabel(text)
        }.largeGapAfter()
      }

      val row = row { checkBox(IdeBundle.message("updates.plugins.settings.checkbox"), settings.state::isPluginsCheckNeeded) }

      if (WhatsNewAction.isAvailable()) {
        row { checkBox(IdeBundle.message("updates.settings.show.editor"), settings.state::isShowWhatsNewEditor) }.largeGapAfter()
      }
      else {
        row.largeGapAfter()
      }

      row {
        cell(isFullWidth = true) {
          if (checkNowEnabled) {
            button(IdeBundle.message("updates.settings.check.now.button")) {
              val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(myLastCheckedLabel))
              val settingsCopy = UpdateSettings()
              settingsCopy.state.copyFrom(settings.state)
              settingsCopy.state.isCheckNeeded = true
              settingsCopy.state.isPluginsCheckNeeded = true
              settingsCopy.selectedChannelStatus = selectedChannel(channelModel.selected)
              UpdateChecker.updateAndShowResult(project, settingsCopy)
              updateLastCheckedLabel(myLastCheckedLabel!!, settings.lastTimeChecked)
            }
          }
          myLastCheckedLabel = contextLabel("").withLargeLeftGap().component
          updateLastCheckedLabel(myLastCheckedLabel!!, settings.lastTimeChecked)
        }
      }.largeGapAfter()

      if (!settings.ignoredBuildNumbers.isEmpty()) {
        row {
          myLink = link(IdeBundle.message("updates.settings.ignored")) {
            val text = settings.ignoredBuildNumbers.joinToString("\n")
            val project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(myLink))
            val result = Messages.showMultilineInputDialog(project, null, IdeBundle.message("updates.settings.ignored.title"), text, null,
                                                           null)
            if (result != null) {
              settings.ignoredBuildNumbers.clear()
              settings.ignoredBuildNumbers.addAll(result.split('\n'))
            }
          }.component
        }.largeGapAfter()
      }

      if (manager != ExternalUpdateManager.TOOLBOX) {
        row(" ") {}
        row { component(SeparatorComponent()) }

        row {
          val font = JBFont.label().asBold()

          val iconLabel = JBLabel(PluginLogo.reloadIcon(AllIcons.Nodes.Toolbox, 40, 40, null))
          iconLabel.verticalAlignment = SwingConstants.TOP

          val panel = JPanel(BorderLayout(JBUI.scale(10), 0))
          panel.add(iconLabel, BorderLayout.WEST)

          val linePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

          val label = JBLabel(IdeBundle.message("updates.settings.recommend.toolbox.first.part"))
          label.border = JBUI.Borders.emptyRight(5)
          label.font = font
          linePanel.add(label)

          val link = ActionLink(ExternalUpdateManager.TOOLBOX.toolName) {
            BrowserUtil.browse(TOOLBOX_URL)
          }
          link.setExternalLinkIcon()
          link.font = font
          linePanel.add(link)

          val subPanel = JPanel(BorderLayout(0, JBUI.scale(3)))
          subPanel.add(linePanel, BorderLayout.NORTH)
          subPanel.add(MultiLineLabel(IdeBundle.message("updates.settings.recommend.toolbox.multiline.description")))

          panel.add(subPanel)
          component(panel)
        }
      }

      var wasEnabled = settings.isCheckNeeded || settings.isPluginsCheckNeeded

      onGlobalApply {
        val isEnabled = settings.isCheckNeeded || settings.isPluginsCheckNeeded
        if (isEnabled != wasEnabled) {
          when {
            isEnabled -> UpdateCheckerComponent.getInstance().queueNextCheck()
            else -> UpdateCheckerComponent.getInstance().cancelChecks()
          }
          wasEnabled = isEnabled
        }
      }
    }
  }

  private fun Cell.contextLabel(@Label buildText: String): CellBuilder<JLabel> {
    val label = label(buildText)
    label.component.foreground = UIUtil.getContextHelpForeground()
    return label
  }

  private fun selectedChannel(value: ChannelStatus?): ChannelStatus = value ?: ChannelStatus.RELEASE

  private fun updateLastCheckedLabel(label: JLabel, time: Long): Unit = when {
    time <= 0 -> {
      label.text = IdeBundle.message("updates.settings.last.check", IdeBundle.message("updates.last.check.never"))
      label.toolTipText = null
    }
    else -> {
      label.text = IdeBundle.message("updates.settings.last.check", DateFormatUtil.formatPrettyDateTime(time))
      label.toolTipText = DateFormatUtil.formatDate(time) + ' ' + DateFormatUtil.formatTimeWithSeconds(time)
    }
  }
}
