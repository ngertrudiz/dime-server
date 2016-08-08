/*
  Copyright (c) 2015-2016 University of Helsinki

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

import fi.hiit.dime.authentication.User;
import fi.hiit.dime.search.WeightedKeyword;

import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import java.util.List;

/** Parent object of all DiMe database objects.
*/
@JsonInclude(value=JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "new", "tagMap"})
@JsonSubTypes({
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Document.class, name="Document"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.ScientificDocument.class, name="ScientificDocument"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Message.class, name="Message"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.Person.class, name="Person"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.FeedbackEvent.class, name="FeedbackEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.BookmarkEvent.class, name="BookmarkEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.WebDocument.class, name="WebDocument"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.DesktopEvent.class, name="DesktopEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.ReadingEvent.class, name="ReadingEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.SummaryReadingEvent.class, name="SummaryReadingEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.MessageEvent.class, name="MessageEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.SearchEvent.class, name="SearchEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.FunfEvent.class, name="FunfEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.CalendarEvent.class, name="CalendarEvent"),
            @JsonSubTypes.Type(value=fi.hiit.dime.data.IntentModelEvent.class, name="IntentModelEvent"),
})
@MappedSuperclass
public class DiMeData extends AbstractPersistable<Long> {
    private static final Logger LOG = 
        LoggerFactory.getLogger(DiMeData.class);

    /** An optional identifying unique string field that can be used
        by the application or logger. The value can be any unique text
        string, and is entirely up to the application developer, but
        make sure it is unique. If you upload another object with the
        same appId later (for the same user) it will replace the old
        one (as long as it is of the same exact class).
    */
    public String appId;

    public void copyIdFrom(DiMeData e) {
        setId(e.getId());
    }

    public void resetId() {
        setId(null);
    }

    /** Date and time when the object was first uploaded via the
        external API - automatically generated by DiMe.
     */
    public Date timeCreated;

    /** Date and time when the objects was last modified via the
        external API - automatically generated by DiMe.
    */
    public Date timeModified;

    /** User associated with the object - automatically generated by
        DiMe.
    */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public User user;

    /** Detailed data type according to the Semantic Desktop ontology:
        http://www.semanticdesktop.org/ontologies/2007/03/22/nfo
     */
    public String type;

    /** Method to call when ever a new object has been uploaded, e.g.
        to clean up user provided data, or perform some house keeping
        before storing in the database.
    */
    public void autoFill() {}

    @Transient
    public Float score;

    @Transient
    public List<WeightedKeyword> weightedKeywords;

    /** List of user-specified tags, interpretation depends on the
        application.
    */
    @OneToMany(cascade = CascadeType.ALL)
    @MapKey(name = "text")
    private Map<String, Tag> tagMap;

    public void setTagMap(Map<String, Tag> tagMap) {
        this.tagMap = tagMap;
    }

    public Map<String, Tag> getTagMap() {
        return tagMap;
    }

    public void setTags(Collection<Tag> tags) {
        if (tags == null)
            return;

        if (tagMap == null)
            tagMap = new HashMap<String, Tag>();
        else 
            tagMap.clear();

        for (Tag t : tags)
            tagMap.put(t.text, t);
    }

    public boolean hasTags() {
        return tagMap != null && tagMap.size() > 0;
    }

    public Collection<Tag> getTags() {
        return tagMap != null ? tagMap.values() : null;
    }

    /** Add a free-form tag to the object.
        @param tagText The tag text to add
    */
    public void addTag(String tagText) {
        addTag(new Tag(tagText));
    }

    /** Add a free-form tag to the object.
        @param tag The tag object to add
    */
    public void addTag(Tag tag) {
        if (tagMap == null)
            tagMap = new HashMap<String, Tag>();
        else if (tagMap.containsKey(tag.text))
            removeTag(tag.text);

        tagMap.put(tag.text, tag);
    }

    /** Remove a tag from the object.
        @param tagText The tag to remove
    */
    public void removeTag(String tagText) {
        if (tagMap != null && tagMap.containsKey(tagText)) {
            Tag tag = tagMap.get(tagText);
            tagMap.remove(tagText);
        }
    }

    /** Return Tag object for a given text tag.
        @param tagText The tag to retrieve
        @return The corresponding Tag object
    */
    public Tag getTag(String tagText) {
        return tagMap != null ? tagMap.get(tagText) : null;
    }

    /** Checks if the object contains a given tag.
        @param tagText Tag to check for
        @return true if tag found, otherwise false
    */
    public boolean hasTag(String tagText) {
        return tagMap != null ? tagMap.containsKey(tagText) : false;
    }

    public static <T extends DiMeData> T makeStub(T data, Class<T> dataType) {
        try {
            T stub = dataType.newInstance();
            stub.setId(data.getId());
            return stub;
        } catch (InstantiationException|IllegalAccessException ex) {
            LOG.error("Unable to create stub of class {}!", dataType.getName());
            return null;
        }
    }
}
