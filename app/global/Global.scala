package global

import play.api._
import scala.collection.JavaConversions._
import play.Play
import play.api.Play.current

import anorm._
import play.api.db.DB
import org.springframework.scheduling.annotation.Async
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import akka.actor.{ActorPath, ActorRef, ActorSystem, Props}
import models.Datasource
import scala.collection.mutable
import actors.SearchProcessorActor


object Global extends GlobalSettings {

  // val searchDataContainer = new SearchDataContainer

  val actorSystem = ActorSystem("Global")

  val searchActors = new mutable.HashMap[String,ActorRef]()
  
  override def onStart(app: Application) {

    Logger.info("Application has started")
    
    val datasourcesConfig = Play.application().configuration().getConfig("datasources")
    
    datasourcesConfig.asMap.keySet.foreach{ key =>
        searchActors.add(key, Global.actorSystem.actorOf(Props( new SearchProcessorActor(key, new Datasource(datasourcesConfig.getConfig(key))) )) )

    }

  }   
  
  def getSearchProvider(field:String) = searchActors.get(field)
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
  
}