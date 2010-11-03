package edu.uci.lighthouse.core.controller;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;

import edu.uci.lighthouse.core.Activator;
import edu.uci.lighthouse.core.util.ModelUtility;
import edu.uci.lighthouse.model.LighthouseClass;
import edu.uci.lighthouse.model.LighthouseEntity;
import edu.uci.lighthouse.model.LighthouseEvent;
import edu.uci.lighthouse.model.LighthouseInterface;
import edu.uci.lighthouse.model.LighthouseModel;
import edu.uci.lighthouse.model.LighthouseModelManager;
import edu.uci.lighthouse.model.LighthouseRelationship;
import edu.uci.lighthouse.model.LighthouseEvent.TYPE;

/**
 * This class is responsible for update the Lighthouse model based on events
 * generated by the developers workspace and by the events that are fetched from database
 * 
 * */
public class UpdateLighthouseModel {

	private static Logger logger = Logger.getLogger(UpdateLighthouseModel.class);
	
	public static void addEvents(Collection<LighthouseEvent> listEvents) {
		LighthouseModelManager LhManager = new LighthouseModelManager(LighthouseModel.getInstance());
		Collection<String> listClassesToRemove = new LinkedHashSet<String>(); 
		// for each entity event
		for (LighthouseEvent event : listEvents) {
			Object artifact = event.getArtifact();
			if (artifact instanceof LighthouseEntity) {
				logger.debug("adding event: " + event.toString());
				LhManager.addEvent(event);
				if (event.getType()==TYPE.REMOVE) {
					if (artifact instanceof LighthouseClass || artifact instanceof LighthouseInterface) {
						LighthouseEntity entity = (LighthouseEntity) artifact;
						String fqn = entity.getFullyQualifiedName();
						if ( event.getAuthor().equals(Activator.getDefault().getAuthor()) 
								|| !ModelUtility.existsInWorkspace(fqn))  {
							listClassesToRemove.add(fqn);
							// I needed to verify whether the class is _not_ on my
							// workspace, because if other developer create a class
							// in his workspace (that is not on my workspace) and
							// later delete it I need to remove this from my visualization
						}
					}
				}
			} else if (artifact instanceof LighthouseRelationship) {
				if (event.getType()==TYPE.REMOVE) {
					LighthouseRelationship rel = (LighthouseRelationship) artifact;
					if (rel.getType()!=LighthouseRelationship.TYPE.INSIDE) {
						LhManager.removeRelationship(rel);
					}
				}
			}
		}
		// for each relationship event
		for (LighthouseEvent event : listEvents) {
			Object artifact = event.getArtifact();
			if (artifact instanceof LighthouseRelationship) {
				logger.debug("adding event: " + event.toString());
				if (event.getType()==TYPE.ADD) {
					LhManager.addEvent(event);	
				}
			}
		}
		LhManager.removeArtifactsAndEvents(listClassesToRemove);
	}
	
}
