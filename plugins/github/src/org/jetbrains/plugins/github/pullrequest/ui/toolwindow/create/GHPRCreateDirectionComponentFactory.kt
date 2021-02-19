// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.github.pullrequest.ui.toolwindow.create

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.MutableCollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import git4idea.GitBranch
import git4idea.GitRemoteBranch
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.github.api.GHRepositoryPath
import org.jetbrains.plugins.github.i18n.GithubBundle
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import org.jetbrains.plugins.github.util.GHProjectRepositoriesManager
import java.awt.event.ActionEvent
import javax.swing.ComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class GHPRCreateDirectionComponentFactory(private val repositoriesManager: GHProjectRepositoriesManager,
                                          private val model: GHPRCreateDirectionModel) {

  fun create(): JComponent {
    val base = LinkLabel<Any>("", null) { comp, _ ->
      chooseBaseBranch(comp, model.baseRepo, model.baseBranch, model::baseBranch::set)
    }.apply {
      isFocusable = true
    }
    val head = LinkLabel<Any>("", null) { comp, _ ->
      chooseHeadRepoAndBranch(comp, model.headRepo, model.headBranch, model::setHead)
    }.apply {
      isFocusable = true
    }

    model.addAndInvokeDirectionChangesListener {
      val baseRepo = model.baseRepo.repository.repositoryPath
      val headRepo = model.headRepo?.repository?.repositoryPath
      val showRepoOwners = headRepo != null && baseRepo != headRepo

      base.text = getRepoText(baseRepo, showRepoOwners, model.baseBranch)
      head.text = getRepoText(headRepo, showRepoOwners, model.headBranch)
    }

    return NonOpaquePanel().apply {
      layout = MigLayout(LC()
                           .gridGap("0", "0")
                           .insets("0", "0", "0", "0"))

      add(base, CC().minWidth("${UI.scale(30)}"))
      add(JLabel(" ${UIUtil.leftArrow()} ").apply {
        foreground = UIUtil.getInactiveTextColor()
        border = JBUI.Borders.empty(0, 5)
      })
      add(head, CC().minWidth("${UI.scale(30)}"))
    }
  }

  private fun chooseBaseBranch(parentComponent: JComponent,
                               currentRepo: GHGitRepositoryMapping,
                               currentBranch: GitRemoteBranch?,
                               applySelection: (GitRemoteBranch?) -> Unit) {
    val branchModel = MutableCollectionComboBoxModel<GitRemoteBranch>().apply {
      val remote = currentRepo.gitRemote.remote
      val branches = currentRepo.gitRemote.repository.branches.remoteBranches.filter {
        it.remote == remote
      }
      replaceAll(branches.toList())
      selectedItem = currentBranch.takeIf { it != null && branches.contains(it) }
    }

    val popup = createPopup(branchModel,
                            GithubBundle.message("pull.request.create.direction.base.repo"),
                            JBTextField(currentRepo.repository.repositoryPath.toString()).apply {
                              isEnabled = false
                            }) {
      applySelection(branchModel.selected)
    }
    popup.showUnderneathOf(parentComponent)
  }

  private fun chooseHeadRepoAndBranch(parentComponent: JComponent,
                                      currentRepo: GHGitRepositoryMapping?,
                                      currentBranch: GitBranch?,
                                      applySelection: (GHGitRepositoryMapping?, GitBranch?) -> Unit) {

    val repoModel = CollectionComboBoxModel(repositoriesManager.knownRepositories.toList(), currentRepo)
    val branchModel = MutableCollectionComboBoxModel<GitBranch>()

    repoModel.addListDataListener(object : ListDataListener {
      override fun intervalAdded(e: ListDataEvent) {}
      override fun intervalRemoved(e: ListDataEvent) {}
      override fun contentsChanged(e: ListDataEvent) {
        if (e.index0 == -1 && e.index1 == -1) updateHeadBranches(repoModel.selected, branchModel)
      }
    })

    updateHeadBranches(repoModel.selected, branchModel)
    if (currentBranch != null && branchModel.items.contains(currentBranch)) branchModel.selectedItem = currentBranch

    val popup = createPopup(branchModel,
                            GithubBundle.message("pull.request.create.direction.head.repo"),
                            ComboBox(repoModel).apply {
                              renderer = SimpleListCellRenderer.create("") {
                                it?.repository?.repositoryPath?.toString()
                              }
                            }) {
      applySelection(repoModel.selected, branchModel.selected)
    }

    popup.showUnderneathOf(parentComponent)
  }

  private fun <T : GitBranch> createPopup(branchModel: ComboBoxModel<T>,
                                          @Nls repoRowMessage: String,
                                          repoComponent: JComponent,
                                          onSave: () -> Unit): JBPopup {
    var buttonHandler: ((ActionEvent) -> Unit)? = null

    val branchComponent = ComboBox(branchModel).apply {
      renderer = SimpleListCellRenderer.create<GitBranch>("", GitBranch::getName)
    }

    val panel = panel(LCFlags.fill) {
      row(repoRowMessage) {
        repoComponent(CCFlags.growX)
      }
      row(GithubBundle.message("pull.request.create.direction.branch")) {
        branchComponent(CCFlags.growX)
      }
      row {
        right {
          button(GithubBundle.message("pull.request.create.direction.save")) {
            buttonHandler?.invoke(it)
          }
        }
      }
    }.apply {
      border = JBUI.Borders.empty(8, 8, 0, 8)
    }

    return JBPopupFactory.getInstance()
      .createComponentPopupBuilder(panel, repoComponent.takeIf { it.isEnabled } ?: branchComponent)
      .createPopup().apply {
        setRequestFocus(true)
      }.also { popup ->
        branchModel.addListDataListener(object : ListDataListener {
          override fun intervalAdded(e: ListDataEvent?) {
            invokeLater { popup.pack(true, false) }
          }

          override fun intervalRemoved(e: ListDataEvent?) {
            invokeLater { popup.pack(true, false) }
          }

          override fun contentsChanged(e: ListDataEvent?) {}
        })

        buttonHandler = {
          onSave()
          popup.closeOk(null)
        }
      }
  }

  private fun updateHeadBranches(repoMapping: GHGitRepositoryMapping?, branchModel: MutableCollectionComboBoxModel<GitBranch>) {
    val repo = repoMapping?.gitRemote?.repository
    if (repo == null) {
      branchModel.replaceAll(emptyList())
      return
    }

    val remote = repoMapping.gitRemote.remote
    val remoteBranches = repo.branches.remoteBranches.filter {
      it.remote == remote
    }

    val branches = repo.branches.localBranches + remoteBranches
    branchModel.replaceAll(branches)
  }

  companion object {

    @NlsSafe
    private fun getRepoText(repo: GHRepositoryPath?, showOwner: Boolean, branch: GitBranch?): String {
      if (repo == null || branch == null) return "Select..."
      return repo.toString(showOwner) + ":" + branch.name
    }
  }
}