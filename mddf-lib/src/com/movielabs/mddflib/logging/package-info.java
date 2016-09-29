/**
 * Provides classes supporting a mutable and hierarchical (i.e., tree-like) log
 * structure. Log entries are tagged with metadata indicating:
 * <ul>
 * <li>the file the message pertains to and (possible) a specific location
 * within that file</li>
 * <li>severity of problem</li>
 * <li>module that generated the log entry</li>
 * <li>MDDF category associated with the entry (e.g., Manifest, Avails, CPE)
 * </li>
 * <li>relevant MDDF Specification, if any (e.g. 'TR-META-AVAIL v2.1')</li>
 * </ul>
 * <p>
 * The classes in this package provide the ability to organize, filter, and sort
 * log entries using the associated metadata. 
 * </p>
 */
package com.movielabs.mddflib.logging;