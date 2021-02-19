// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.idea.maven.server.wsl

import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.idea.maven.server.MavenDistribution
import org.jetbrains.idea.maven.server.MavenRemoteProcessSupportFactory
import org.jetbrains.idea.maven.server.MavenRemoteProcessSupportFactory.MavenRemoteProcessSupport
import org.jetbrains.idea.maven.server.RemotePathTransformerFactory
import org.jetbrains.idea.maven.utils.MavenLog
import org.jetbrains.idea.maven.utils.MavenWslUtil
import org.jetbrains.idea.maven.utils.MavenWslUtil.getDefaultMavenDistribution

class WslMavenRemoteProcessSupportFactory : MavenRemoteProcessSupportFactory {
  override fun create(jdk: Sdk,
                      vmOptions: String?,
                      mavenDistribution: MavenDistribution?,
                      project: Project,
                      debugPort: Int?): MavenRemoteProcessSupport {
    val wslDistribution = project.basePath?.let { WslPath.getDistributionByWindowsUncPath(it) }
                          ?: throw IllegalArgumentException("Project $project is not WSL based!")
    //todo: replace this with settings
    val tempDistribution = wslDistribution.getDefaultMavenDistribution() ?: throw IllegalStateException("Maven is not installed on WSL")
    MavenLog.LOG.info("Use maven distribution at ${tempDistribution.pathToMaven}")
    return WslMavenServerRemoteProcessSupport(wslDistribution, jdk, vmOptions, tempDistribution, project, debugPort)
  }

  override fun isApplicable(project: Project): Boolean {
    return MavenWslUtil.useWslMaven(project)
  }
}

class WslRemotePathTransformFactory : RemotePathTransformerFactory {
  override fun isApplicable(project: Project): Boolean {
    return MavenWslUtil.useWslMaven(project)
  }

  override fun createTransformer(project: Project): RemotePathTransformerFactory.Transformer {
    val wslDistribution = MavenWslUtil.tryGetWslDistribution(project)
                          ?: throw IllegalArgumentException("Project $project is not WSL based!")
    return object : RemotePathTransformerFactory.Transformer {
      override fun toRemotePath(localPath: String): String? {
        return wslDistribution.getWslPath(localPath)
      }

      override fun toIdePath(remotePath: String): String? {
        return wslDistribution.getWindowsPath(remotePath)
      }
    }

  }

}