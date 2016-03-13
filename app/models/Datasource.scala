package models

import play.api._
import scala.collection.JavaConversions._
import play.api.Play.current
import play.api.db.DB
import anorm._
import java.text.SimpleDateFormat
import scala.collection.Traversable
import play.api.Configuration
import java.io.Serializable


class Datasource(val config:Configuration) {
	
  private var lastUpdateTimestamp = System.currentTimeMillis()
  private var iteration = 0
  
  private val mysqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  
  
  private var autoIncrementId:Int = 0
  private var orderCounter:Int = 0
  
  implicit lazy val connection = DB.getConnection()
  
  def getIteration = iteration
  
  /**
   * Returns either a complete update or only updated records from the last update
   */
  def findAll(getModifiedOnly:Boolean = true):(Boolean,Stream[models.Row]) = {
		orderCounter = 0
		
		val additionalConditions = if( iteration == 0 || !getModifiedOnly ) ""
			else config
				  	.getStringList("modified")
				  	.map( _.map( "( %s > \"%s\" )".format(_, mysqlDateFormat.format( lastUpdateTimestamp )) ).mkString(" OR ") )
            .getOrElse("")

		
		iteration += 1
		
		lastUpdateTimestamp = System.currentTimeMillis()
		
		val completeDataset = additionalConditions.isEmpty

		( completeDataset, SQL(buildQuery(additionalConditions)).apply().map( mapToRow ))

	    
  }

  private def buildQuery(condition:String = ""):String = {
    
    val configWhere = config.getString("conditions").map( List(_) ).getOrElse(List())
    
    val conditions:List[String]  =  if( condition.isEmpty ) configWhere else configWhere ++ List(condition)
    
    val defaultFields:Seq[String] = Seq(config.getString("id").getOrElse("id"), config.getString("text").getOrElse("name"))
    
    val fields = defaultFields
    
    val q = "SELECT "+fields.mkString(", ") + 
    		" FROM " + config.getString("table").getOrElse("table") +
    		( if( conditions.isEmpty ) "" else " WHERE " + conditions.mkString(" AND ") ) +
    		config.getString("order").map( " ORDER BY "+_ ).getOrElse("")

    q
    
  }
  
  private def mapToRow(row:anorm.Row):models.Row = {
	  val rowList = row.asList
	  
	  val id = rowList.head match {
	    case Some(s:String) => s.toInt
	    case s:String => s.toInt
	    case i:Int => i
	    case _ =>
	      autoIncrementId+=1
	      autoIncrementId 

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
    config.getBoolean("clean_text").getOrElse(false)
  }
  
}
