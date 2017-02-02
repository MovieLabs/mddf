/**
 * Provides components for translation of <tt>Avails</tt> formatted as an XML
 * Document into an Excel workbook. The bulk of the work is done by a
 * <tt>XlsxBuilder</tt> instance.
 * 
 * <p>
 * The actions of the <tt>XlsxBuilder</tt> are governed by the
 * <tt>mappings.json</tt> file. This specifies how the XML elements are
 * mapped to spreadsheet cells. The mappings are defined using the following
 * syntax:
 * <uL>
 *
 * <li>it defines XPaths that identify the XML source for an Excel cell based on
 * the column id (e.g., "AvailTrans:PriceValue")</li>
 * <li>The '{}' indicates what the appropriate namespace is for an element
 * (e.g., '{md}' indicates that the software should insert the namespace prefix
 * for the Common Metadata namespace)</li>
 * <li>a JSON array indicates any of the included XPaths may be used. For
 * example:
 * 
 * <pre>
            "AvailTrans:End":
            [
                "{avail}End",
                "{avail}EndCondition"
            ]
 * </pre>
 * 
 * indicates that either of the two XPaths may be used to obtain the value for
 * AvailTrans:End</li>
 * 
 * <li>a JSON Object indicates the correct XPath to use is dependent on the
 * WorkType of the Asset. Example:
 * 
 * <pre>
            "AvailMetadata:ReleaseYear":
            {
                "Season": "{avail}SeasonMetadata/{avail}ReleaseDate",
                "Episode": "{avail}EpisodeMetadata/{avail}ReleaseDate"
            }
 * </pre>
 * 
 * </li>
 * <li>An '*' at the start of an XPath indicates that multiple XML values may be
 * mapped to a single Excel cell via the use of a comma separated list. Example:
 * "AvailTrans:HoldbackLanguage": "*{avail}HoldbackLanguage"</li>
 * </ul>
 * </p>
 * 
 * @author L. Levin, Critical Architectures LLC
 *
 */
package com.movielabs.mddflib.avails.xlsx;