/*
  Copyright (c) 2015 University of Helsinki

  Permission is hereby granted, free of charge, to any person
  obtaining a copy of this software and associated documentation files
  (the "Software"), to deal in the Software without restriction,
  including without limitation the rights to use, copy, modify, merge,
  publish, distribute, sublicense, and/or sell copies of the Software,
  and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be
  included in all copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/

package fi.hiit.dime.data;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
   A summary reading event.
    Summary reading events contain more "refined" data, while non-summary reading events contain more detail.
    Summary reading events can contain united (non-floating) rectangles (but can contain floating rectangles).
    Non-summary reading events contain floating rectangles only. 

   Also see https://github.com/HIIT/PeyeDF/wiki/Data-Format/.
*/
@Entity
public class SummaryReadingEvent extends ReadingEvent {

    /** Proportion of document which was displayed in viewports.
     * Probably seen by the user, but not guaranteed to have been read.
     */
    public Double proportionSeen;

    /** Proportion of document which was read
     */
    public Double proportionRead;

    /** Proportion of document which was marked as "interesting"
     */
    public Double proportionInteresting;

    /** Proportion of document which was marked as "critical"
     */
    public Double proportionCritical;

    /** Total time spent reading for the whole session (seconds)
     */
    public Double readingTime;

    /** List of strings that were searched for, found and selected by user, during this reading session.
     */
    @ElementCollection(targetClass = String.class)
    public List<String> foundStrings;

}
