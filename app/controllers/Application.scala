package controllers

import play.api._
import play.api.mvc._
import global.Global
import views.html.defaultpages.badRequest
import play.api.libs.json.Json

object Application extends Controller {
  
  def index = Action {
    // Ok(views.html.index("Your new application is ready."))
    Ok("Hello World!")
  }
  
  def search(query:String, limit:Int = 1000) = Action {
    
    if( !query.isEmpty()){
    	val search = Global.getSearchProvider()
    	    
    	val results = search.search(query)
    	
    	Ok( Json.toJson(results) )
    	
    } else {
      BadRequest("Search string is empty")
    }
    

  }
}