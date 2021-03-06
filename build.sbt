// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "mimir_community_clean"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

//persistLauncher in Compile := true

//persistLauncher in Test := false

//EclipseKeys.skipParents in ThisBuild := false

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature",
          "-language:postfixOps", "-language:implicitConversions",
          "-language:higherKinds", "-language:existentials")

val scalajsReactVersion = "0.11.3"
val scalaCSSVersion = "0.5.1"

val app = crossProject.settings(
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value  / "shared" / "main" / "scala",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % "0.5.4",
    "com.lihaoyi" %%% "upickle" % "0.2.7",
	"org.scalatest" %%% "scalatest" % "3.0.1" % Test
  ),
  scalaVersion := "2.11.8"
).jsSettings(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
	"com.olvind" %%% "scalajs-react-components" % "0.6.0",
	"com.github.japgolly.scalajs-react" %%% "extra" % scalajsReactVersion,
	"com.github.japgolly.scalacss"      %%% "core"     % scalaCSSVersion,
    "com.github.japgolly.scalacss"      %%% "ext-react" % scalaCSSVersion
  )
).jvmSettings(
  //fork := true,
  //outputStrategy in run := Some(StdoutOutput),
  //connectInput in run := true,
  //javaOptions += "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005",
  mainClass in (Compile, run) := Some("ubodin.MimirCommunityServer"),
  libraryDependencies ++= Seq(
    "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
	"org.eclipse.jetty" % "jetty-http" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-io" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-security" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-server" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-servlet" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-servlets" % "9.3.11.v20160721",
	"org.eclipse.jetty" % "jetty-util" % "9.3.11.v20160721",
	"org.eclipse.jetty.websocket" % "websocket-server" % "9.3.11.v20160721",
	"org.bouncycastle" % "bcpkix-jdk15on" % "1.55",
	"org.bouncycastle" % "bcprov-jdk15on" % "1.55",
	"org.apache.commons" % "commons-dbcp2" % "2.1.1",
	"org.apache.commons" % "commons-lang3" % "3.5",
	"commons-logging" % "commons-logging" % "1.2",
	"org.apache.commons" % "commons-pool2" % "2.4.2",
	"com.itextpdf" % "itextpdf" % "5.5.9",
	"org.spire-math" %% "jawn-parser" % "0.7.0",
	"joda-time" % "joda-time" % "2.9.4",
	"mysql" % "mysql-connector-java" % "6.0.6",
	"org.scalikejdbc" %% "scalikejdbc" % "2.4.2",
	"org.scalikejdbc" %% "scalikejdbc-config" % "2.4.2",
	"org.scalikejdbc" %% "scalikejdbc-core" % "2.4.2",
	"org.scalikejdbc" %% "scalikejdbc-interpolation" % "2.4.2",
	"org.scalikejdbc" %% "scalikejdbc-jsr310" % "2.4.2",
	"com.lihaoyi" %%% "upickle" % "0.2.7",
	"com.lihaoyi" %%% "scalatags" % "0.5.4",
	//"org.slf4j" % "slf4j-api" % "1.7.21",
	//"org.slf4j" % "slf4j-simple" % "1.7.21",
	"org.scala-js"   %% "scalajs-test-interface" % "0.6.14",
	"org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    "org.xerial"     %   "sqlite-jdbc"  % "3.16.1",
  	"javax.media"	%   "jai_core"	% "1.1.3" from "http://download.osgeo.org/webdav/geotools/javax/media/jai_core/1.1.3/jai_core-1.1.3.jar",  
    "info.mimirdb" %% "mimir-core" % "0.2" exclude("javax.media", "jai_core"),
  	//spark ml
    "org.apache.spark" % "spark-sql_2.11" % "2.2.0",
    "org.apache.spark" % "spark-mllib_2.11" % "2.2.0",
  	"net.sf.py4j" % "py4j" % "0.10.4",
  	"org.scala-lang.modules" %% "scala-xml" % "1.0.6"
  ),
  resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  resolvers += Resolver.mavenLocal,
  resolvers += "MimirDB" at "http://maven.mimirdb.info/"
)


lazy val appJS = app.js
lazy val appJVM = app.jvm.settings(
  (resources in Compile) += (fastOptJS in (appJS, Compile)).value.data
)

import sbtdocker._
enablePlugins(DockerPlugin)
dockerfile in docker := {
    val userDataVolMountPoint = "/usr/local/source/"
    val sbtPath = s"${userDataVolMountPoint}sbt/bin/sbt"
	val initContainer = Seq(
		s"if [ -e ${userDataVolMountPoint}initComplete ]",
		"then", 
		    "echo 'already initialized...'",
			s"(cd ${userDataVolMountPoint}mimir_community_clean; git pull; ${sbtPath} appJVM/run)",
		"else",
			s"""curl -sL "https://github.com/sbt/sbt/releases/download/v0.13.15/sbt-0.13.15.tgz" | gunzip | tar -x -C ${userDataVolMountPoint}""",
			s"chmod 0755 ${sbtPath}",
			s"git clone https://github.com/UBOdin/mimir.git ${userDataVolMountPoint}mimir",
			s"git clone https://github.com/mrb24/mimir_community_clean.git ${userDataVolMountPoint}mimir_community_clean",
			s"(cd ${userDataVolMountPoint}mimir; git checkout -b FixGProMMetadataLookup origin/FixGProMMetadataLookup; ${sbtPath} publish)",
			s"touch ${userDataVolMountPoint}initComplete",
			"echo 'initialization complete...'",
			s"(cd ${userDataVolMountPoint}mimir_community_clean; git pull; ${sbtPath} appJVM/run)",
		"fi"
		)
		
	val instructions = Seq(
	  sbtdocker.Instructions.From("docker.mimirdb.info/alpine_oraclejdk8"),
	  sbtdocker.Instructions.Volume(Seq(s"type=volume,source=mimir-vol,target=${userDataVolMountPoint}")),
	  sbtdocker.Instructions.Run.exec(Seq("apk", "add", "--no-cache", "bash")),
	  sbtdocker.Instructions.Run.exec(Seq("apk", "add", "--no-cache", "curl")),
	  sbtdocker.Instructions.Run.exec(Seq("apk", "add", "--no-cache", "git")),
	  sbtdocker.Instructions.Run.exec(Seq("mkdir", s"${userDataVolMountPoint}")),
	  sbtdocker.Instructions.Run(s"""( ${initContainer.map(el => s"""echo "$el" >> ${userDataVolMountPoint}initContainer.sh; """).mkString("") } chmod 0755 ${userDataVolMountPoint}initContainer.sh)"""),
	  sbtdocker.Instructions.EntryPoint.exec(Seq("/bin/bash", "-c", s"${userDataVolMountPoint}initContainer.sh"))
	)
	Dockerfile(instructions)
}



