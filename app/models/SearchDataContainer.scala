package models

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.collection.mutable.HashSet

class SearchDataContainer {
	
	private var data:Map[Int,Row] = new HashMap[Int,Row]
	private val nGramLength = 3
	
	private var nGramHashToRowMapping:Map[String,Set[Int]] = new HashMap
  
	def addRow(id:Int, text:String){
	  data synchronized {
	    val row =  data.getOrElseUpdate(id, { new Row(text)})
	    
	    nGramHashToRowMapping synchronized {
    		row.getNGrams(nGramLength) foreach( (hash:String) => {
    		  var idSet = nGramHashToRowMapping.getOrElseUpdate( hash, { new HashSet[Int]() })
    		  idSet synchronized {
    			  idSet.add(id)
    		  }
    		})
	    }
	    
	  }
	}
	
	def getRow(id:Int):Option[Row] = {
	  data synchronized {
		  data.get(id)
	  }
	  
	}
	
	def search( text:String, limit:Int = 1000):Option[scala.collection.immutable.Set[Int]] = {
	  val searchText = text.toLowerCase().trim()
	  val nGramHashes = new Row(text).getNGrams( nGramLength ) 
	  
	  val items = nGramHashes		
			.map( (nGramHash) => nGramHashToRowMapping.get(nGramHash) ) // find all companies with word n-grams
			.filter( !_.isEmpty ) // filter empty matches
			.map( _.get ) // get a set of row Ids

	  if( items.isEmpty){
	    None
	  } else {		
		Some(items
			.reduceLeft( _ intersect _ ) // intersect all lists and get a list of companies with all n-grams
			.filter( getRow(_) match { case Some(r:Row) => r.data.contains(text) case _ => false } )
			.toSet)
	  }

	}
	
	
}

class Row(val data:String){
  
  def getNGrams(n:Int):Iterator[String] = {
	  data.sliding(n)
  }
  
  /*
  def getNGramHashes(n:Int):Iterator[Int] = {
    getNGrams(n) map( _.hashCode() )
  }
  */
  
  def equals(dataToCheck:String):Boolean = {
    data.equals(dataToCheck)
  }
  
}

