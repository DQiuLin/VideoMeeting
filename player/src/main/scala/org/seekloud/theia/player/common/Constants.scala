package org.seekloud.theia.player.common

import java.io.File

/**
  * User: TangYaruo
  * Date: 2019/9/23
  * Time: 14:20
  */
object Constants {

  val cachePath: String = s"${System.getProperty("user.home")}/.theia/pcClient"
  val cacheFile = new File(cachePath)

  if (!cacheFile.exists()) cacheFile.mkdirs()

  val mp4Path: String = System.getProperty("user.home") + "\\.theia\\pcClient\\mp4"

  val mp4 = new File(mp4Path)
  if (!mp4.exists()) mp4.mkdirs()







}
