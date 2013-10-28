package models

import scala.collection.concurrent
import play.api.db.DB
import global.Global
import akka.actor.Props
import actors.messages._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import actors.messages._
import play.Configuration
import scala.concurrent.duration.Duration
import scala.collection.mutable.Set


/*
class SearchProvider {
  
  private val fields = new concurrent.TrieMap[String,SearchDataContainer]()
  
  def get(field:String):Option[SearchDataContainer] = fields.get(field)
  def add(field:String, value:SearchDataContainer) = fields.update(field, value) 
  def remove(field:String) = fields.remove(field)
}
*/




