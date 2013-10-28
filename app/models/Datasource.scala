package models

import play.api._
import scala.collection.JavaConversions._
import play.Play
import play.api.Play.current
import play.api.db.DB
import anorm._
import java.text.SimpleDateFormat
import scala.collection.Traversable
import play.Configuration


class Datasource(val config:Configuration) {
	
  private var lastUpdateTimestamp = System.currentTimeMillis()
  private var iteration = 0
  
  private val mysqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  
  
  private var autoIncrementId:Int = 0
  private var orderCounter:Int = 0
  
  implicit val connection = DB.getConnection()
  
  def getIteration = iteration
  
  /**
   * Returns either a complete update or only updated records from the last update
   */
  def findAll(getModifiedOnly:Boolean = true):Tuple2[Boolean,Stream[models.Row]] = {
		orderCounter = 0
		
		val additionalConditions = if( iteration == 0 || !getModifiedOnly ) ""
			else config
				  	.getStringList("modified", List.empty[String])
				  	.map( "( %s > \"%s\" )".format(_, mysqlDateFormat.format( lastUpdateTimestamp )) )
				  	.mkString(" OR ")
		
		iteration += 1
		
		lastUpdateTimestamp = System.currentTimeMillis()
		
		val completeDataset = additionalConditions.isEmpty

		( completeDataset, SQL(buildQuery(additionalConditions)).apply().map( mapToRow ))

	    
  }

  private def buildQuery(condition:String = ""):String = {
    
    val configWhere = if( config.getString("conditions","").isEmpty() ) List() else List(config.getString("conditions","") )
    
    val conditions:List[String]  =  if( condition.isEmpty ) configWhere else configWhere ++ List(condition)
    
    val defaultFields:Seq[String] = Seq(config.getString("id", "id"), config.getString("text","name"))
    
    val fields = defaultFields
    
    val q = "SELECT "+fields.mkString(", ") + 
    		" FROM " + config.getString("table", "table") +
    		( if( conditions.isEmpty ) "" else " WHERE " + conditions.mkString(" AND ") ) +
    		( if( config.getString("order", "").isEmpty ) "" else " ORDER BY "+config.getString("order", "") )
    // println(q)
    q
    
  }
  
  private def mapToRow(row:anorm.SqlRow):models.Row = {
	  val rowList = row.asList
	  
	  val id = rowList(0) match { 
	    case Some(s:String) => s.toInt
	    case s:String => s.toInt
	    case i:Int => i
	    case _ => { 
	      autoIncrementId+=1
	      autoIncrementId 
	    } 
	  }
	  val value = rowList(1) match { 
	    case Some(s:String) => if( hasCleanTextOption ) Row.cleanText( s ) else ""
	    case s:String => if( hasCleanTextOption ) Row.cleanText( s ) else ""
	    case _ => "" 
	  }
	  
	  // create an order column
	  val priority = if( rowList.length > 2 ) rowList(2) match { 
		    case Some(s:String) => s.toInt
		    case s:String => s.toInt
		    case i:Int => i
		    case _ => { 
		      orderCounter+=1
		      orderCounter
		    }
	  } else {
	      orderCounter+=1
	      orderCounter
	  }
	  
//	  println("#"+id+ " " + rowList(1) + " :" + orderCounter + " -> " + value )
	  
	  new models.Row(id, value, priority, iteration )    
  }
  
  def hasCleanTextOption:Boolean = {
    config.getBoolean("clean_text",  false)
  }
  
}

object Datasource {
  
}