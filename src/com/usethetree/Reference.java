/*
 * Copyright 2014 NH Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.usethetree;

import java.util.HashMap;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Iterable;

import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Reference implements Iterable<Reference>{

  // There are rules and there are (valid) exceptions: It is ACTUALLY REALLY better to not use getters and setters here:
  public String elemName;
  public String value;
  public HashMap<Key, Reference> children = new HashMap<Key, Reference>();
  private HashMap<String, Integer> repeatingElementsIndex = new HashMap<String, Integer>(); 
  public Reference parent = null;
  public Reference firstChild = null;
  public Reference lastChild = null;
  public Reference nextSibling = null;
  public Reference prevSibling = null;
   
 

  public Reference(String elemName) {
    this.elemName = elemName;
  }

  public Reference(String elemName, String value) {
	    this.elemName = elemName;
	    this.value = value;
	  }
  
  public Reference(Reference parent, String elemName, String value) {
	  this.parent = parent;
	  this.elemName = elemName;
	  this.value = value;
	 }
  
  public Reference addChild(String key, String elemName, String value) {
	  Reference child = new Reference(this, elemName, value);
	  
	  int repeatingElemIndex = 0;			// The first (repeating) element with name XYZ
	  Reference tmp = this.firstChild(key);
	  Integer i = null;
	  if (tmp!=null) {
			  i = repeatingElementsIndex.get(key);
		  if (i==null)
			  repeatingElemIndex=1;					// The second
		  else
			  repeatingElemIndex=i+1;				// third, ...
	  }
	  
	  if (!this.children.isEmpty()) {
		  child.prevSibling=this.lastChild;
		  this.lastChild.nextSibling=child;
		  this.lastChild=child;
		  
	  } else {
		  this.firstChild=child;
		  this.lastChild=child;
	  }
		  
	  this.children.put(new Key(key, repeatingElemIndex), child);
	  if (repeatingElemIndex>0)
		  repeatingElementsIndex.put(key, repeatingElemIndex);
	  
      return child;
  }
  
  public Reference addChild(String key, String elemName) {
	  return addChild(key, elemName, null);
  }

  public Reference addChild(String elemName) {
	  return addChild(elemName, elemName, null);
  }
  
  public Reference set(String key, String elemName) {			// move to first child OR CREATE

	  Reference tmp = this.children.get(new Key(key));
	  if (tmp==null)
		  return this.addChild(key, elemName);
	  else
		  return tmp;
	  
  }
  
  public Reference firstChild(String key) {					// move to first child OR NULL

	 return this.children.get(new Key(key));
	  
  }
  
  public Reference child(String elemName, int index) {

	  return this.children.get(new Key(elemName, index));

  }
  
  public Reference firstChild(String elemName, String key, String value) {
	  
	  Reference child = this.firstChild(elemName);
	  if (child!=null) {
		  Reference curChild = child.firstChild(key);
		  while (curChild!=null) {
			  if (curChild.value.equals(value))
				  return child;
			  if (curChild.nextSibling!=null&&curChild.nextSibling.elemName.equals(key))
				  curChild=curChild.nextSibling;
			  else 
				curChild=null;  
		 }
	  }
	
	  return null;
  }
  
public Reference set(String elemName) {
	  
	  Reference tmp =  this.firstChild(elemName);
	  if (tmp==null)
			return this.addChild(elemName);
	  else
		  return tmp;
  }
  
  public Reference set(String key, String elemName, String value) {
	  
	  Reference tmp = this.firstChild(key);
	  if (tmp==null)
			return this.addChild(key, elemName, value);
	  else
		  return tmp;
  }
  
 public Reference add(String elemName, String value) {
	  
	  Reference tmp = this.firstChild(elemName);
	  if (tmp==null)
			return this.addChild(elemName, elemName, value);
	  else {
		   tmp.value = "" + (Integer.parseInt(tmp.value) + Integer.parseInt(value));
		   return tmp;
	  }	   
  }
  
  
  public Reference moveWhere(String elemName, String key, String value) {
	  Reference tmp = firstChild(elemName, key, value);
	  if (tmp!=null)
		  return tmp;
	  else
		  return this.addChild(elemName);
  }  
  
  public void removeFieldFromChildren(String childrenName, String key) {
	  
	  Reference curChild = this.firstChild(childrenName);
	  
	  while (curChild!=null) {
	  
		  Reference tmpReference = curChild.firstChild(key);
		  if (tmpReference!=null)
			  curChild.children.remove(tmpReference);
		  if (curChild.nextSibling!=null&&curChild.nextSibling.elemName.equals(childrenName))
			  curChild=curChild.nextSibling;
		  else 
			  curChild=null;  
		  
	  }  
	 
  }
  
  
  
  public Reference addNextSibling(String elemName) {
	  return this.parent.addChild(elemName);
  }

  
  @Override
  public Iterator<Reference> iterator() {
	  
      return new ChildrenIterator(this);
  }
  
  
  public static Reference createReferenceFromXML(InputStream in) throws XMLStreamException  {
		
	    XMLStreamReader xmlStreamReader = null;
	    int eventType = 0;
	    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

	    xmlStreamReader = xmlInputFactory.createXMLStreamReader(in);	
			
	    // when XMLStreamReader is created, 
	    // it is positioned at START_DOCUMENT event.
	    eventType = xmlStreamReader.getEventType();

//		int depth = 0;
		
		String curElem = "";
		String curValue = "";
		final Reference inputRoot = new Reference("InputRoot");	
		
		Reference curTree = inputRoot;
		boolean lastTagWasAnOpeningTag = false;
		
	  	
			while(xmlStreamReader.hasNext()) {
			    eventType = xmlStreamReader.next();
			    
			    switch (eventType) {
			        case XMLStreamConstants.START_ELEMENT:				        	
			        	curElem = xmlStreamReader.getLocalName();
			        	curTree = curTree.addChild(curElem);
			        	lastTagWasAnOpeningTag = true;
//			        	depth+=1;
			            break;
			        case XMLStreamConstants.END_ELEMENT:
			        	if (lastTagWasAnOpeningTag)
			        		curTree.value = curValue;
			        	curTree = curTree.parent;     	
			        	lastTagWasAnOpeningTag = false;
//			        	depth-=1;
			            break;
			        case XMLStreamConstants.PROCESSING_INSTRUCTION:
			            break;
			        case XMLStreamConstants.CHARACTERS:
			        	curValue = xmlStreamReader.getText();
			            break;
			        case XMLStreamConstants.COMMENT:
			            break;
			        case XMLStreamConstants.START_DOCUMENT:
			            break;
			        case XMLStreamConstants.ATTRIBUTE:
			        	break;
			        case XMLStreamConstants.END_DOCUMENT:
			            break;
			        case XMLStreamConstants.ENTITY_REFERENCE:
			            break;
			        case XMLStreamConstants.DTD:
			            break;
			        case XMLStreamConstants.CDATA:
			            break;
			        case XMLStreamConstants.SPACE:
			            break;
			        default:   	
			    }
			}
			return inputRoot;

	  }

  public static void writeXMLFromReference(Reference ref, String filename, HttpServletResponse response) throws XMLStreamException, IOException  {
		

	  	StringBuilder type = new StringBuilder("attachment; filename=");
		type.append(filename);
		
		//response.setContentLength( - not known, since writing out streaming - );
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", type.toString());
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(
	               response.getOutputStream() );
		
		writer.writeStartDocument();
		
		boolean backingOut=false;
		boolean doLoop=true;
		while(doLoop) {
			
			if (!backingOut) {
				writer.writeStartElement(ref.elemName);
				if (ref.value!=null) {
					writer.writeCharacters(ref.value);
					writer.writeEndElement();
				}
			}	
			
			if (!backingOut&&!ref.children.isEmpty()) {
				ref = ref.firstChild;
			} else {
				if (ref.nextSibling!=null) {
					ref = ref.nextSibling;
					backingOut=false;
				} else {
					if (ref.parent!=null ) {
						ref = ref.parent;
						writer.writeEndElement();
						backingOut=true;
					} else
						doLoop=false;
				}
			}
			
		}
						
		writer.writeEndDocument();
		writer.flush();
		writer.close();

	}
  
}