package actors

import akka.actor.{ActorRef, Props, Actor}
import actors.messages._
import akka.event.slf4j.Logger
import global.Global
import models.{Datasource, Row}
import scala.concurrent.duration._
import actors.messages.RemoveRowsFromIteration
import actors.messages.InsertRowAction
import actors.messages.DeleteRowAction
import actors.messages.UpdateRowAction
import scala.Some
import play.api.libs.concurrent.Akka
import scala.collection.mutable.Set
import scala.collection.concurrent
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._



class SearchProcessorActor(val name:String, val datasource:Datasource) extends Actor{

  private val container = new SearchDataContainer(datasource)

  private val initialLoadDuration = 1.seconds

  private val updateInterval = datasource.config.getLong("update_interval").getOrElse(0L).seconds
  private val fullUpdateInterval = datasource.config.getLong("full_update_interval").getOrElse(0L).seconds


  override def preStart() = {

    if( updateInterval.toSeconds > 0 ){
      context.system.scheduler.schedule( updateInterval, updateInterval)( updateIntervalFired(true) )
    }

    if(fullUpdateInterval.toSeconds > 0){
      context.system.scheduler.schedule( initialLoadDuration, fullUpdateInterval)( updateIntervalFired(false) )
    }
  }

  def updateIntervalFired(getModifiedOnly:Boolean = true):Unit = {

    // Logger.debug("Update iteration: "+name+" (" + ( if(getModifiedOnly) "partial" else "full" ) + ") - " + datasource.getIteration)

    try {
      val currentIteration = datasource.getIteration
      datasource.findAll(getModifiedOnly) match {
        case (isCompleteDataset:Boolean , stream:Stream[Row] ) => {

          stream.foreach( self ! UpdateRowAction(_) )
          if( isCompleteDataset ){
            self ! RemoveRowsFromIteration(currentIteration)
          }
        }
      }

    } catch {
      case e:Throwable => println("Exception in data update "+e)
    }


  }



  def receive = {
    case SearchRowsRequest(str:String, limit:Int) => {
      val results = container.searchIds(str, limit)
      sender ! SearchRowsResult(results)
    }
    case InsertRowAction(row:Row) => container.addRow(row)
    case UpdateRowAction(row:Row) => container.updateRow(row)
    case DeleteRowAction(row:Row) => container.removeRow(row)
    case DeleteRowAction(rowId:Int) => container.removeRowById(rowId)
    case RemoveRowsFromIteration(iteration:Int) => container.removeRowsFromIteration(iteration)
  }

}


class SearchDataContainer(datasource:Datasource) {

  private val data = new concurrent.TrieMap[Int,Row]()
//  private val data = scala.collection.mutable.HashMap[Int,Row]()
  private val nGramLength = 3


  private val nGramHashToRowMapping = new concurrent.TrieMap[String,scala.collection.mutable.Set[Int]]
//  private val nGramHashToRowMapping = scala.collection.mutable.HashMap[String,scala.collection.mutable.Set[Int]]()


  // TODO schedule updates
  def addRow(row:Row) = updateRow(row)

  def updateRow(row:Row) = {

    // remove existing hashes first
    removeRowById(row.getId)

    // data synchronized {
    val newRow =  data.getOrElseUpdate(row.getId , row)

    // nGramHashToRowMapping synchronized{

    newRow.getNGrams(nGramLength) foreach( (hash:String) => {
      val idHash = nGramHashToRowMapping.getOrElseUpdate( hash, { scala.collection.mutable.Set[Int]() })
      idHash synchronized {
        idHash.add(row.getId)
      }
    })
    // }
    // }
  }

  def removeRowById(id:Int) = {
    getRow(id) match {
      case Some(row:Row) => {
        removeNGramHashes( row.getId, row.getNGrams(nGramLength) )
        // data synchronized{
        data.remove(row.getId)
        // }

      }
      case None => Unit
    }
  }

  def removeNGramHashes(rowId:Int, ngrams:Iterator[String]) = {
    // nGramHashToRowMapping synchronized {
    ngrams.foreach(
      nGramHashToRowMapping.get(_) match {
        case Some(x:scala.collection.mutable.Set[Int]) => x.remove(rowId)
        case _ => Unit
      } )
    // }
  }

  def getRow(id:Int):Option[Row] = {
    // data synchronized {
    data.get(id)
    // }

  }

  def removeRow(row:Row) = {
    removeRowById(row.getId)
  }

  def removeRowsFromIteration(iteration:Int) = {
    // println("Remove Rows From Iteration "+iteration)

    // data synchronized {
    data
      .filter( tuple => tuple._2.getIteration == iteration )
      .foreach( tuple => removeRowById( tuple._1 ) )

    // }

  }


  def searchIds( text:String, limit:Int=1000) = searchRows(text, limit).map( _.getId )

  def searchRows( text:String, limit:Int = 1000):scala.collection.immutable.Seq[Row] = {

    val search = if( datasource.hasCleanTextOption ) Row.cleanText( text ) else text

    val items = // nGramHashToRowMapping synchronized {
      Row.getNGrams(search, nGramLength )
        .map( (nGramHash) => nGramHashToRowMapping.get(nGramHash) ) // find all companies with word n-grams
        .filter( _.isDefined ) // filter empty matches
        .map( _.get ) // get a set of row Ids
    // }



    if( items.isEmpty){
      scala.collection.immutable.Seq[Row]()
    } else {

      items
        .reduceLeft( _ intersect _ ) // intersect all lists and get a list of companies with all n-grams
        .filter( getRow(_).exists(  _.contains(search) ) )
        .toSeq
        .map( getRow )
        .filter( _.isDefined )
        .map( _.get )
        .sortWith( ( row1:Row, row2:Row) => row1.compareTo(row2) )
        .take(limit)
        .toIndexedSeq

    }



  }


}
