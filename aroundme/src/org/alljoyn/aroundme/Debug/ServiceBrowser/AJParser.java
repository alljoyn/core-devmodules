/******************************************************************************
* Copyright (c) 2013, AllSeen Alliance. All rights reserved.
*
*    Permission to use, copy, modify, and/or distribute this software for any
*    purpose with or without fee is hereby granted, provided that the above
*    copyright notice and this permission notice appear in all copies.
*
*    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
*    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
*    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
*    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
*    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
*    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
*    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
******************************************************************************/
package org.alljoyn.aroundme.Debug.ServiceBrowser;

/*
 * Utilities for parsing DBus/AllJoyn Introspection data
 */





import java.util.ArrayList;

// XML Parser-related
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//
// Class for supporting parsing of Introspection XML
//
public class AJParser  {


	// Class global variables - mostly for keeping track of the XML DOM tree parsing

	DocumentBuilder        mBuilder;
	DocumentBuilderFactory mFactory ;
	Document               mDocument ;
	Element                mRoot ;          // root of the DOM tree
	Node                   mCurrNode ;      // placeholder for current node in tree
	NodeList               mCurrChildren ;  // children of current node

	String outline ;

	// constant for the DBUS Introspection DTD text
	private final static String DOC_TYPE = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">";


	// Constructor for supplied Introspection data
	AJParser (String introspectData) {
		
		  try {

			  // Parse the DTD
			  mFactory = DocumentBuilderFactory.newInstance();
			  mBuilder = mFactory.newDocumentBuilder();
			  mDocument = mBuilder.parse(new InputSource(new StringReader(introspectData.replace(DOC_TYPE, ""))));
			  mDocument.getDocumentElement().normalize();
			  mRoot = mDocument.getDocumentElement();
			  
			  // get the initial list of children and initialise the current node pointer

			  mCurrChildren = mRoot.getChildNodes();
			  if (mCurrChildren.getLength() > 0){
				  mCurrNode = mCurrChildren.item(0);
			  } else {
				  mCurrNode = null ;
			  }
		  } catch (Throwable t) { // generic catch for any exceptions. Really should be more specific!
			  t.printStackTrace();
		  }

		
	}// AJParser(String)
	
	public NodeList getChildNodes(){ 
		return mCurrChildren;
	} //getChildNodes
	
//------------------------------------------------------------------ 
  /** Little routine to output a line to the text area **/
  protected void addLine (String str) {
      outline += str + "\n";

      // for now,  print to stderr
      System.out.println (str);		
  } 



  //------------------------------------------------------------------ 
  // Parsing routines
  //------------------------------------------------------------------ 
  
  /*
   * DBus introspection data is returned as an XML document with the following general form:
   * 

        <!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN"
         "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
        <node name="/org/freedesktop/sample_object">
          <interface name="org.freedesktop.SampleInterface">
            <method name="Frobate">
              <arg name="foo" type="i" direction="in"/>
              <arg name="bar" type="s" direction="out"/>
              <arg name="baz" type="a{us}" direction="out"/>
              <annotation name="org.freedesktop.DBus.Deprecated" value="true"/>
            </method>
            <method name="Bazify">
              <arg name="bar" type="(iiu)" direction="in"/>
              <arg name="bar" type="v" direction="out"/>
            </method>
            <method name="Mogrify">
              <arg name="bar" type="(iiav)" direction="in"/>
            </method>
            <signal name="Changed">
              <arg name="new_value" type="b"/>
            </signal>
            <property name="Bar" type="y" access="readwrite"/>
          </interface>
          <node name="child_of_sample_object"/>
          <node name="another_child_of_sample_object"/>
       </node>

   */
  
  //------------------------------------------------------------------ 
  /* 
   * parses a simple name attribute (<xyz name="blah, blah, blah"/>) and returns the value
   */
  public String parseNameAttr(Node node){
	  String str = ""; // return value
	  int i;
	  NamedNodeMap attrList;

	  str = "";
	  
	  // get the attributes
	  attrList = node.getAttributes();
	  Node node2 = attrList.getNamedItem("name");
	  if (null!=node2) {  // lookup by name worked (remember, "name" is optional)
		  //str = node2.getNodeValue() + " ";
		  str = node2.getNodeValue();
	  }

	  return str;	  
  }

  
  //------------------------------------------------------------------ 
  /* 
   * parses a set of attributes with a name attribute and arbitrary other attributes
   * We ignore the "name tag", but include the other descriptive tags
   * i.e.we return a string of the type "text" a1=abc, a2=def 
   */
  public String parseAttrList(Node node){
	  String str = ""; // return value
	  int i, numAttr;
	  NamedNodeMap attrList;
	  String nodeName ;

	  // get the attributes
	  attrList = node.getAttributes();
	  str = "";
	  numAttr = attrList.getLength();
	  
	  // process the "name" attribute first, then scan through the other attributes
	  if (numAttr>0){
		  Node node2 = attrList.getNamedItem("name");
		  if (null!=node2) {  // lookup by name worked (remember, "name" is optional)
			  str = node2.getNodeValue() + " ";
		  }
			  
		  for (i=0;i<numAttr;i++) {
			  nodeName=attrList.item(i).getNodeName();
			  if (!("name".equals(nodeName))){
				  str=str + attrList.item(i).getNodeName() + "=" + attrList.item(i).getNodeValue();
			  }
			  if (i<(numAttr-1)) str = str + ",";  // add comma except for last item
		  }
	  }

	  return str;	  
  }


  //------------------------------------------------------------------ 
  /* 
   * parses the Node that represents a method definition 
   * general format is 
   *        <method name="Frobate">
              <arg name="foo" type="i" direction="in"/>
              <arg name="bar" type="s" direction="out"/>
              <arg name="baz" type="a{us}" direction="out"/>
              <annotation name="org.freedesktop.DBus.Deprecated" value="true"/>
            </method>
            
            the return frmat is something like:
            method Frobate (foo type=i, direction=in; bar type="s" direction="out;
                            baz type=a{us} direction=out; 
                            annotation org.freedesktop.DBus.Deprecated value=true)
   */
  public String parseMethod(Node node, String prefix){
	  String str ; // return value
	  
	  //str = prefix + "method " + parseNameAttr(node) + "(";
	  str = prefix + parseNameAttr(node) + "(";
	  
	  // scan through the sub-nodes and
	  // process the <arg> and <annotation> declarations
	  NodeList children = node.getChildNodes();
	  int numChildren = children.getLength();
	  for (int i=0;i<numChildren;i++) {
		  Node node2 = children.item(i);
		  if (Node.ELEMENT_NODE != node2.getNodeType()) {
			  continue; // skip anything that isn't an ELEMENT
		  }
		  String nodeName = node2.getNodeName();  
		  if ("arg".equals(nodeName)){
			  str = str + parseAttrList (node2) ;
			  if (i<(numChildren-1)) str = str + ";";
		  } else if ("annotation".equals(nodeName)){
			  str = str + parseAnnotation (node2, "");
		  }
	  } // end for
	  

	  str = str + ")\n";
	  
	  return str;	  
  }
//------------------------------------------------------------------ 
  /* 
   * parses the Node that represents a <signal> definition 
   * general format is 
   *        <signal name="Changed">
              <arg name="new_value" type="b"/>
            </signal>

   */
  public String parseSignal(Node node, String prefix){
	  String str ; // return value
	  //str = prefix + "signal " + parseNameAttr(node) + "(";
	  str = prefix + parseNameAttr(node) + "(";

	  // scan through the sub-nodes and
	  // process the <arg> and <annotation> declarations
	  NodeList children = node.getChildNodes();
	  int numChildren = children.getLength();
	  for (int i=0;i<numChildren;i++) {
		  Node node2 = children.item(i);
		  if (Node.ELEMENT_NODE != node2.getNodeType()) {
			  continue; // skip anything that isn't an ELEMENT
		  }
		  String nodeName = node2.getNodeName();  
		  if ("arg".equals(nodeName)){
			  str = str + parseAttrList (node2) ;
			  if (i<(numChildren-1)) str = str + ";";
		  }
	  } // end for
	  

	  str = str + ")\n";

	  return str;	  
  }
//------------------------------------------------------------------ 
  /* 
   * parses the Node that represents a <property> definition 
   * general format is 
   *        <property name="Bar" type="y" access="readwrite"/>
   */
  public String parseProperty(Node node, String prefix){
	  String str ; // return value

	  //str = prefix + "property " ;
	  str = prefix;
	  str = str + node.getNodeValue() + " " + parseAttrList(node) + "\n" ;

	  return str;	  
  }
//------------------------------------------------------------------ 
  /* 
   * parses the Node that represents an <annotation> definition 
   * general format is 
   *        <annotation name="org.freedesktop.DBus.Deprecated" value="true"/>
   */
  public String parseAnnotation(Node node, String prefix){
	  String str = ""; // return value

	  str = prefix + "annotation " ;
	  str = str + node.getNodeName() + " " + parseAttrList(node) + "\n" ;
	  
	  return str;	  
  }

//------------------------------------------------------------------ 
  /* 
   * parses the Node that represents a <node> definition embedded within the current <node>
   * general format is 
   *        <node name="child_of_sample_object"/>
   */
  public String parseSubNode(Node node, String prefix){
	  String str = ""; // return value

	  str = prefix + "node " ;
	  str = str + node.getNodeName() + " " + parseNameAttr(node) + "\n" ;
 
	  return str;	  
  }
//------------------------------------------------------------------ 
  
  //------------------------------------------------------------------ 

  /* 
   * parses the Node that represents an interface definition 
   * general format is <interface name=abcdef> followed by <method>, <signal> and <property> definitions
   * This version returns an ArrayList of Strings for methods, signals and properties
   */

  public void parseInterface(Node node, ArrayList<String> methodList, ArrayList<String> signalList, ArrayList<String> propertyList)
  {
	  String nodeName ;

	  // scan through the sub-nodes and
	  // process the <method>, <property> and <signal> declarations
	  // adding each to the appropriae array
	  NodeList children = node.getChildNodes();
	  int numChildren = children.getLength();
	  for (int i=0;i<numChildren;i++) {
		  Node node2 = children.item(i);
		  nodeName = node2.getNodeName();

		  if (Node.ELEMENT_NODE != node2.getNodeType()) {
			  continue; // skip anything that isn't an ELEMENT
		  }

		  if ("method".equals(nodeName)){
			  methodList.add(parseMethod (node2, "")) ;
		  } 
		  else if ("signal".equals(nodeName)){
			  signalList.add(parseSignal (node2, ""));
		  } 
		  else if ("property".equals(nodeName)){
			  propertyList.add(parseProperty (node2, ""));
		  }
	  } // end for

  }
  
  /* 
   * parses the Node that represents an interface definition 
   * general format is <interface name=abcdef> followed by <method>, <signal> and <property> definitions
   * This version returns a String with formatted lists of methods, signals and properties
   */
  public String parseInterface(Node node, String prefix){
	  String str = ""; // return value
	  String nodeName ;
	  ArrayList<String> methodList   = new ArrayList<String>();
	  ArrayList<String> signalList   = new ArrayList<String>();
	  ArrayList<String> propertyList = new ArrayList<String>();

	  parseInterface (node, methodList, signalList, propertyList);

	  //OK scan back through each array and build the descriptive string
	  int i;
	  str = "Methods:\n";
	  if (methodList.isEmpty()){
		  str = str + "(none)\n";
	  } else {
		  for (i=0; i<methodList.size(); i++){
			  str = str + methodList.get(i);
		  }
	  }

	  str = str + "\nSignals:\n";
	  if (signalList.isEmpty()){
		  str = str + "(none)\n";
	  } else {
		  for (i=0; i<signalList.size(); i++){
			  str = str + signalList.get(i);
		  }
	  }

	  str = str + "\nProperties:\n";
	  if (propertyList.isEmpty()){
		  str = str + "(none)\n";
	  } else {
		  for (i=0; i<propertyList.size(); i++){
			  str = str + propertyList.get(i);
		  }
	  }

	  str = str + "\n";

	  return str;	  
  }


  /* 
   * parses the Node that represents an interface definition 
   * general format is <interface name=abcdef> followed by <method>, <signal> and <property> definitions
   * This version returns a String with formatted lists of methods, signals and properties
   */
  public void parseInterface(Node node, ArrayList<String> IntfList){

	  ArrayList<String> methodList   = new ArrayList<String>();
	  ArrayList<String> signalList   = new ArrayList<String>();
	  ArrayList<String> propertyList = new ArrayList<String>();

	  parseInterface (node, methodList, signalList, propertyList);

	  //OK scan back through each array and build the combined array
	  int i;
	  IntfList.add("Methods:");
	  if (methodList.isEmpty()){
		  IntfList.add("(none)\n");
	  } else {
		  for (i=0; i<methodList.size(); i++){
			  IntfList.add(methodList.get(i));
		  }
	  }

	  IntfList.add("\nSignals:\n");
	  if (signalList.isEmpty()){
		  IntfList.add("(none)\n");
	  } else {
		  for (i=0; i<signalList.size(); i++){
			  IntfList.add(signalList.get(i));
		  }
	  }

	  IntfList.add("\nProperties:\n");
	  if (propertyList.isEmpty()){
		  IntfList.add("(none)\n");
	  } else {
		  for (i=0; i<propertyList.size(); i++){
			  IntfList.add(propertyList.get(i));
		  }
	  }

	  IntfList.add("\n");
  
  }


  //------------------------------------------------------------------ 

  /** Parse the service Introspection data and return a descriptive text string **/
//this method is deprecated, just use the constructor and individual functions directly in applications
  
  private String parseService (String introspectData) {

	  String str ; // string for building the return

	  // variables for parsing the Introspection XML data
	  DocumentBuilder builder;

	  str = "" ; // top level node does not seem to have a name

	  try {


		  // Parse the DTD
		  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		  builder = factory.newDocumentBuilder();
		  Document document = builder.parse(new InputSource(new StringReader(introspectData.replace(DOC_TYPE, ""))));
		  document.getDocumentElement().normalize();

		  String nodeName ;
		  String prefix = "  ";  // dumb way to add indenting

		  Element root = document.getDocumentElement();
		  nodeName = root.getNodeName();
		  str = nodeName + " " + parseNameAttr(root) + "\n";
		  
		  // scan through the elements

		  NodeList children = root.getChildNodes();
		  for (int i=0;i<children.getLength();i++) {
			  Node node = children.item(i);
			  if (Node.ELEMENT_NODE != node.getNodeType()) {
				  continue; // skip anything that isn't an ELEMENT
			  }

//TODO: really should put each level into it's own list
//      Violates the MVC desire, but easier than implementing a parallel tree for display
			  nodeName = node.getNodeName();
			  if ("interface".equals(nodeName)) {
				  str = str + parseInterface (node, prefix);
			  }
			  else if ("node".equals(nodeName)) {
				  str = str + parseSubNode (node, prefix);
			  }

		  } // end for
	  
		  
	  } catch (Throwable t) { // generic catch for any exceptions. Really should be more specific!
		  t.printStackTrace();
	  }
	  
	  return str ;
  } // parseService



} // AJParser
