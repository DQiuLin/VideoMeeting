name := "VideoMeeting"

val scalaV = "2.12.10"

val projectName = "VideoMeeting"

val projectVersion = "0.1.0"

def commonSettings = Seq(
  version := projectVersion,
  scalaVersion := scalaV,
  scalacOptions ++= Seq(
    //"-deprecation",
    "-feature"
  ),
  javacOptions ++= Seq("-encoding", "UTF-8")
)

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val protocol = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(commonSettings: _*)

lazy val protocolJvm = protocol.jvm
lazy val protocolJs = protocol.js

lazy val shared = (project in file("shared"))
  .settings(name := "shared")
  .settings(commonSettings: _*)

lazy val rtpClient = (project in file("rtpClient"))
  .settings(name := "rtpClient")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.akkaSeq,
    libraryDependencies ++= Dependencies.backendDependencies)
  .dependsOn(shared)

val playerMain = "videomeeting.player.Boot"
lazy val player = (project in file("player")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(playerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "player")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("player" -> playerMain),
    packJvmOpts := Map("player" -> Seq("-Xmx256m", "-Xms64m")),
    packExtraClasspath := Map("player" -> Seq("."))
  )
  .settings(
    //    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.bytedecoLibs,
    libraryDependencies ++= Dependencies4Player.playerDependencies,
  )
  .dependsOn(protocolJvm, rtpClient)

lazy val webClient = (project in file("webClient"))
  .enablePlugins(ScalaJSPlugin)
  .settings(name := "webClient")
  .settings(commonSettings: _*)
  .settings(
    inConfig(Compile)(
      Seq(
        fullOptJS,
        fastOptJS,
        packageJSDependencies,
        packageMinifiedJSDependencies
      ).map(f => (crossTarget in f) ~= (_ / "sjsout"))
    ))
  .settings(skip in packageJSDependencies := false)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //mainClass := Some("com.neo.sk.virgour.front.Main"),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.8.0",
      "io.circe" %%% "circe-generic" % "0.8.0",
      "io.circe" %%% "circe-parser" % "0.8.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "com.lihaoyi" %%% "scalatags" % "0.6.7" withSources(),
      "org.seekloud" %%% "byteobject" % "0.1.1",
      "in.nvilla" %%% "monadic-html" % "0.4.0-RC1" withSources()
    )
  )
  .dependsOn(protocolJs)

val meetingManagerMain = "videomeeting.meetingManager.Boot"
lazy val meetingManager = (project in file("meetingManager")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(meetingManagerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "meetingManager")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("meetingManager" -> meetingManagerMain),
    packJvmOpts := Map("meetingManager" -> Seq("-Xmx64m", "-Xms32m")),
    packExtraClasspath := Map("meetingManager" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  )
  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in webClient).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in webClient).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in webClient).value,
    watchSources ++= (watchSources in webClient).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm)

val tmpMain = "videomeeting.tmp.Boot"
lazy val tmp = (project in file("tmp")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(meetingManagerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "tmp")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("tmp" -> meetingManagerMain),
    packJvmOpts := Map("tmp" -> Seq("-Xmx64m", "-Xms32m")),
    packExtraClasspath := Map("tmp" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  )
  .settings {
    (resourceGenerators in Compile) += Def.task {
      val fastJsOut = (fastOptJS in Compile in webClient).value.data
      val fastJsSourceMap = fastJsOut.getParentFile / (fastJsOut.getName + ".map")
      Seq(
        fastJsOut,
        fastJsSourceMap
      )
    }.taskValue
  }
  .settings((resourceGenerators in Compile) += Def.task {
    Seq(
      (packageJSDependencies in Compile in webClient).value
      //(packageMinifiedJSDependencies in Compile in frontend).value
    )
  }.taskValue)
  .settings(
    (resourceDirectories in Compile) += (crossTarget in webClient).value,
    watchSources ++= (watchSources in webClient).value
  )
  .settings(scalaJSUseMainModuleInitializer := false)
  .dependsOn(protocolJvm)

val rtpServerMain = "videomeeting.rtpServer.Boot"

lazy val rtpServer = (project in file("rtpServer")).enablePlugins(PackPlugin)
  .settings(commonSettings: _*)
  .settings(
    mainClass in reStart := Some(rtpServerMain),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(name := "rtpServer")
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("rtpServer" -> rtpServerMain),
    packJvmOpts := Map("rtpServer" -> Seq("-Xmx512m", "-Xms32m")),
    packExtraClasspath := Map("rtpServer" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies
  ).dependsOn(protocolJvm, shared, rtpClient)