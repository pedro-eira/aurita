name := """aurita"""
organization := "meetsatori.com"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.3"
lazy val macwireVersion = "2.3.0"
lazy val taggingVersion = "1.0.0"
lazy val playSlickVersion = "3.0.3"
lazy val mysqlVersion = "8.0.8-dmr"
lazy val playSilhouetteVersion = "5.0.3"
lazy val ficusVersion = "1.4.3"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  ehcache,
  ws,
  specs2                      % Test,
  "com.softwaremill.macwire" %% "macros"                   % macwireVersion          % "provided",
  "com.softwaremill.macwire" %% "util"                     % macwireVersion,
  "com.softwaremill.common"  %% "tagging"                  % taggingVersion,
  "com.typesafe.play"        %% "play-slick"               % playSlickVersion,
  "com.typesafe.play"        %% "play-slick-evolutions"    % playSlickVersion,
  "mysql"                     % "mysql-connector-java"     % mysqlVersion,
  "com.mohiva" %% "play-silhouette"                        % playSilhouetteVersion,
  "com.mohiva" %% "play-silhouette-persistence"            % playSilhouetteVersion,
  "com.mohiva" %% "play-silhouette-password-bcrypt"        % playSilhouetteVersion,
  "com.mohiva" %% "play-silhouette-crypto-jca"             % playSilhouetteVersion,
  "com.mohiva"               %% "play-silhouette-testkit"  % playSilhouetteVersion % "test",
  "com.iheart"               %% "ficus"                    % ficusVersion
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "meetsatori.com.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "meetsatori.com.binders._"
