package global

import play.api._
import scala.collection.JavaConversions._
import play.api.Play.current

import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.{ActorPath, ActorRef, ActorSystem, Props}
import models.Datasource
import scala.collection.mutable
import actors.SearchProcessorActor


object Global extends GlobalSettings {

  // val searchDataContainer = new SearchDataContainer

  val actorSystem = ActorSystem("Global")

  var searchActors = Map[String,ActorRef]()
  
  override def onStart(app: Application) {

    Logger.info("Application has started")


    searchActors = Play.current.configuration.getConfig("datasources").map( {
      config:Configuration => config.subKeys.map( key => {
        val datasource = new Datasource(config.getConfig(key).get)
        key -> Global.actorSystem.actorOf(Props( new SearchProcessorActor(key, datasource) ))
      }).toMap
    }).getOrElse(Map())


  }   
  
  def getSearchProvider(field:String) = searchActors.get(field)
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
  
}