package edu.uci.lighthouse.core.dbactions.push;

import java.util.Collection;

import edu.uci.lighthouse.core.controller.PushModel;
import edu.uci.lighthouse.model.LighthouseEvent;
import edu.uci.lighthouse.model.LighthouseModel;
import edu.uci.lighthouse.model.jpa.JPAException;

public class FileEventAction extends AbstractEventAction {

	public FileEventAction(Collection<LighthouseEvent> events) {
		super(events);
	}

	@Override
	public void run() throws JPAException {
		LighthouseModel lhModel = LighthouseModel.getInstance();
		new PushModel(lhModel).saveEventsInDatabase(getEvents());
	}

}