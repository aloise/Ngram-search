package global

import play.api._
import play.Play
import play.api.Play.current
import models.SearchDataContainer
import anorm._
import play.api.db.DB

object Global extends GlobalSettings {

  val searchDataContainer = new SearchDataContainer
  
  override def onStart(app: Application) {
    Logger.info("Application has started")
    
    // load the initial data
    val sql = Play.application().configuration().getString("application.datasource.completeSQL")
    
    DB.withConnection( implicit connection => { 
      
	    val anormSql = SQL(sql)
	    val data = anormSql().foreach( row => {
	      
	      val rowList = row.asList
	      
	      val id = rowList(0).toString.toInt
	      val value = rowList(1).toString
	      
	      searchDataContainer.addRow(id, value)
	      
	      // println("#"+id+" "+value)
	      
	    })      
      
    })
    
  }   
  
  def getSearchProvider() = searchDataContainer
  
  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }  
  
}