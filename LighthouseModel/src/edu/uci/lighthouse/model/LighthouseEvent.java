package edu.uci.lighthouse.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.log4j.Logger;

/**
 *  
 * */
@NamedQueries ({
	@NamedQuery(name = "LighthouseEvent.findByTimestamp",
				query = "SELECT event " + 
						"FROM LighthouseEvent event " + 
						"WHERE event.timestamp >= :timestamp " +
						"OR event.committedTime >= :timestamp") 
})
/**
 * Represents a event (add, remove, modify) generated by a user.
 * 
 * @author nilmax
 * 
 */
@Entity
public class LighthouseEvent {
	
	private static Logger logger = Logger.getLogger(LighthouseEvent.class);

	@SequenceGenerator(name="Event_Gen", sequenceName="Event_Gen")
	@Id @GeneratedValue(generator="Event_Gen")
	private Integer id;

	/** User that generates the event. */
	@OneToOne(cascade = CascadeType.ALL)
	private LighthouseAuthor author;

	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp = new Date(0);

	@OneToOne(cascade = CascadeType.ALL)
	private LighthouseEntity entity = null;

	@OneToOne(cascade = CascadeType.ALL)
	private LighthouseRelationship relationship = null;
	
	private boolean isCommitted = false; 
	
	private Date committedTime = new Date(0);
	
	/** Type of the event. */
	public static enum TYPE {
		ADD, REMOVE, MODIFY, CUSTOM
	}

	private TYPE type;	

	protected LighthouseEvent() {
	}

	public LighthouseEvent(TYPE type, LighthouseAuthor author, LighthouseEntity entity) {
		this.type = type;
		this.author = author;
		this.setArtifact(entity);
	}
	
	public LighthouseEvent(TYPE type, LighthouseAuthor author, LighthouseRelationship relationship) {
		this.type = type;
		this.author = author;
		this.setArtifact(relationship);
	}

	protected void setId(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}
	
	public TYPE getType() {
		return type;
	}

	/**
	 * @return the user that generates the event.
	 */
	public LighthouseAuthor getAuthor() {
		return author;
	}

	public void setAuthor(LighthouseAuthor author) {
		this.author = author;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	/** @return Can return NULL*/
	public Date getTimestamp() {
		return timestamp;
	}

	public Object getArtifact() {
		if (entity == null && relationship == null) {
			logger.error(this.toString() + " has a NULL artifact");
			return null;
		} else if (entity != null) {
			return entity;
		} else {
			return relationship;
		}
	}

	public void setArtifact(LighthouseEntity entity) {
		if (relationship == null) {
			this.entity = entity;
		} else {
			logger.warn("Trying to set Entity Artifact when already have a Relationship Artifact: " + this.toString());
		}
	}

	public void setArtifact(LighthouseRelationship relationship) {
		if (entity == null) {
			this.relationship = relationship;
		} else {
			logger.warn("Trying to set Relationship Artifact when already have a Entity Artifact: " + this.toString());
		}
	}

	public void setCommitted(boolean committed) {
		this.isCommitted = committed;
	}

	public boolean isCommitted() {
		return isCommitted;
	}

	public void setCommittedTime(Date committedTime) {
		this.committedTime = committedTime;
		isCommitted = true;
	}

	public Date getCommittedTime() {
		return committedTime;
	}

	@Override
	public String toString() {
		if (getArtifact()!=null) {
			return "LighthouseEvent [author=" + author + ", type=" + type
			+ ", artifact=" + getArtifact().toString() + ", timestamp="
			+ timestamp + "]";
		} else {
			String result = "LighthouseEvent [author="
				+ author + ", type=" + type + ", timestamp=" + timestamp
				+ "]";
			logger.error("Artifact is NULL: " + result);
			return result;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		result = prime * result + ((entity == null) ? 0 : entity.hashCode());
		result = prime * result
				+ ((relationship == null) ? 0 : relationship.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	
	//TODO I removed the timestamp from the equals() because I had some problems to compare 2 EQUAL models that was generated in different times
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LighthouseEvent other = (LighthouseEvent) obj;
		if (author == null) {
			if (other.author != null)
				return false;
		} else if (!author.equals(other.author))
			return false;
		if (entity == null) {
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		if (relationship == null) {
			if (other.relationship != null)
				return false;
		} else if (!relationship.equals(other.relationship))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
