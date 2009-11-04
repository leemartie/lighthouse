package edu.uci.lighthouse.core.controller;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.svnclientadapter.ISVNInfo;

import edu.uci.lighthouse.core.Activator;
import edu.uci.lighthouse.core.listeners.IJavaFileStatusListener;
import edu.uci.lighthouse.core.listeners.IPluginListener;
import edu.uci.lighthouse.core.listeners.ISVNEventListener;
import edu.uci.lighthouse.core.parser.IParserAction;
import edu.uci.lighthouse.core.parser.LighthouseParser;
import edu.uci.lighthouse.model.LighthouseClass;
import edu.uci.lighthouse.model.LighthouseDelta;
import edu.uci.lighthouse.model.LighthouseEntity;
import edu.uci.lighthouse.model.LighthouseEvent;
import edu.uci.lighthouse.model.LighthouseFile;
import edu.uci.lighthouse.model.LighthouseModel;
import edu.uci.lighthouse.model.LighthouseModelManager;
import edu.uci.lighthouse.model.LighthouseRelationship;
import edu.uci.lighthouse.model.LighthouseEvent.TYPE;
import edu.uci.lighthouse.model.jpa.LHEventDAO;

public class Controller implements ISVNEventListener, IJavaFileStatusListener,
		IPluginListener, Runnable {

	private static Logger logger = Logger.getLogger(Controller.class);

	private HashMap<String, Date> mapClassFqnToLastRevisionTimestamp = new HashMap<String, Date>();

	// ....
	private Queue<Map<IFile, ISVNInfo>> pendingWorkingCopyModifications = new LinkedList<Map<IFile, ISVNInfo>>();

	private HashMap<String, LighthouseFile> classBaseVersion = new HashMap<String, LighthouseFile>();

	private List<String> classWithErrors = new LinkedList<String>();

	private Date lastDBAccess = null;

	private boolean threadRunning;

	private final int threadTimeout = 10000;

	@Override
	public void start(BundleContext context) throws Exception {
		loadPreferences();
		loadMap(); // FIXME: delete this later. just for demo purposes
		loadModel();
//		(new Thread(this)).start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		threadRunning = false;
		savePreferences();
	}

	@SuppressWarnings("unchecked")
	public void loadPreferences() {
		String prefix = Activator.PLUGIN_ID;
		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();

		lastDBAccess = new Date(prefStore.getLong(prefix + "lastDBAccess"));
		logger.debug("loading lastDBAccess=" + lastDBAccess);

		try {
			final String filename = "lighthouseWorkingCopy.version";
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					filename));
			mapClassFqnToLastRevisionTimestamp = (HashMap<String, Date>) ois
					.readObject();
			logger.debug("loading " + filename);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void savePreferences() {
		String prefix = Activator.PLUGIN_ID;
		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();

		prefStore.setValue(prefix + "lastDBAccess", lastDBAccess.getTime());
		logger.debug("saving lastDBAccess=" + lastDBAccess);

		try {
			final String filename = "lighthouseWorkingCopy.version";
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(filename));
			oos.writeObject(mapClassFqnToLastRevisionTimestamp);
			logger.debug("saving " + filename);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * Refresh the LighthouseModel with new events from database and fire this changes to the UI.
	 */
	public synchronized void refreshModelBasedOnLastDBAccess() {

		/*
		 * If the map's size = 0, it means that it is the first time that user
		 * is running Lighthouse. Then, the LighthouseModel will be updated only
		 * if the user execute a checkout first.
		 */
		if (mapClassFqnToLastRevisionTimestamp.size() != 0) {
			PullModel pullModel = new PullModel(LighthouseModel.getInstance());
			List<LighthouseEvent> events = pullModel.getNewEventsFromDB(lastDBAccess);
			fireModificationsToUI(events);
		}
		
		LHEventDAO evtDao = new LHEventDAO();
		logger.debug("lastDBAccess: "+lastDBAccess+" current: "+evtDao.getCurrentTimestamp());
		lastDBAccess = evtDao.getCurrentTimestamp();
		
	}
	
	private void loadMap() {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date committedDate = formatter.parse("2009-11-03 20:30:00");
			
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Account", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.ATM", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.ATMCaseStudy", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.BalanceInquiry", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.BankDatabase", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.CashDispenser", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Deposit", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.DepositSlot", committedDate);			
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Keypad", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Screen", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Transaction", committedDate);
			mapClassFqnToLastRevisionTimestamp.put("edu.prenticehall.deitel.Withdrawal", committedDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadModel() {
		
		
		// Esse eh o certo, mas nao sei se funciona
		PullModel pullmodel = new PullModel(LighthouseModel.getInstance());
		pullmodel.executeQueryAfterCheckout(mapClassFqnToLastRevisionTimestamp);
		
		//FIXME: fire the model changed to UI
		LighthouseModel.getInstance().fireModelChanged();
		
//		Date minimumDate = new Date();
//		for (Entry<String, Date> entry : mapClassFqnToLastRevisionTimestamp.entrySet()) {
//			Date revisionTime = entry.getValue();
//			if (revisionTime.before(minimumDate)) {
//				minimumDate = revisionTime;
//			}
//		}
//		
//		PullModel pullModel = new PullModel(LighthouseModel.getInstance());
//		List<LighthouseEvent> events = pullModel.getNewEventsFromDB(new Date(0));
//		
//		pullModel.removeCommittedEvents();
//		
//		fireModificationsToUI(events);
//		
//		
		
		
	}
	
	public synchronized void refreshModelBasedOnWorkingCopy() {
		if (pendingWorkingCopyModifications.size() > 0) {
			Map<IFile, ISVNInfo> svnFiles = pendingWorkingCopyModifications
					.poll();
			mapClassFqnToLastRevisionTimestamp = refreshWorkingCopy(svnFiles);

			PullModel pullModel = new PullModel(LighthouseModel.getInstance());
			pullModel
					.executeQueryAfterCheckout(mapClassFqnToLastRevisionTimestamp);
		}
	}

	@Override
	public void change(final IFile iFile, boolean hasErrors) {
		// refreshModelBasedOnWorkingCopy();

		final String classFqn = getClassFullyQualifiedName(iFile);

		if (hasErrors) {
			// FIXME: criar LighthouseFile a partir do Modelo
			classWithErrors.add(classFqn);
		} else {
			try {
				BufferedReader d = new BufferedReader(new InputStreamReader(
						iFile.getContents()));
				while (d.ready()) {
					System.out.println(d.readLine());
				}

				final LighthouseFile currentLhFile = new LighthouseFile();
				LighthouseParser parser = new LighthouseParser();
				parser.executeInAJob(currentLhFile, Collections
						.singleton(iFile), new IParserAction() {
					@Override
					public void doAction() {
						LighthouseFile baseLhFile = classBaseVersion
								.get(classFqn);
						LighthouseDelta delta = new LighthouseDelta(Activator
								.getDefault().getAuthor(), baseLhFile,
								currentLhFile);

						logger.debug("current: " + currentLhFile.getEntities());
						logger.debug("current: " + currentLhFile.getRelationships()+"\n");
						
						logger.debug("base: " + baseLhFile.getEntities());
						logger.debug("base: " + baseLhFile.getRelationships()+"\n");
						
						logger.debug("delta: " + delta.getEvents());

						PushModel pushModel = new PushModel(LighthouseModel
								.getInstance());
						try {
							pushModel.updateModelFromDelta(delta);
						} catch (Exception e) {
							// TODO: Try to throw up this exception
							logger.error(e);
						}

						// Updates the current base version
						classBaseVersion.put(classFqn, currentLhFile);

						fireModificationsToUI(delta.getEvents());

					}
				});
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void close(IFile iFile, boolean hasErrors) {
		// refreshModelBasedOnWorkingCopy();
		final String classFqn = getClassFullyQualifiedName(iFile);
		classBaseVersion.remove(classFqn);
	}

	@Override
	public void open(final IFile iFile, boolean hasErrors) {
		// refreshModelBasedOnWorkingCopy();

		final String classFqn = getClassFullyQualifiedName(iFile);
		logger.debug("open "+classFqn);
		
		if (hasErrors) {
			// FIXME: criar LighthouseFile a partir do Modelo
			classWithErrors.add(classFqn);
		} else {
			try {
				final LighthouseFile lhFile = new LighthouseFile();
				LighthouseParser parser = new LighthouseParser();
				parser.executeInAJob(lhFile, Collections.singleton(iFile),
						new IParserAction() {
							@Override
							public void doAction() {
								logger.debug("base: "+classFqn+" LHFile entities:"+lhFile.getEntities().size());
								classBaseVersion.put(classFqn, lhFile);
							}
						});
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	@Override
	public void checkout(Map<IFile, ISVNInfo> svnFiles) {
		pendingWorkingCopyModifications.offer(svnFiles);

		// Debug purposes only
		List<String> cNames = getClassesFullyQualifiedName(svnFiles);
		logger.debug("checkout fqns:" + cNames);

		// FIXME: Fire model events to show in the LH view
		System.out.println("");
	}

	@Override
	public void commit(Map<IFile, ISVNInfo> svnFiles) {
		refreshWorkingCopy(svnFiles);
		try {
			PushModel pushModel = new PushModel(LighthouseModel.getInstance());
			pushModel.updateCommittedEvents(
					getClassesFullyQualifiedName(svnFiles), Activator
							.getDefault().getAuthor().getName());
			
			// Refresh is needed because, we need to cleanup the committed events
			refreshModelBasedOnLastDBAccess(); 
		} catch (Exception e) {
			logger.error(e);
		}
	}

	@Override
	public void update(Map<IFile, ISVNInfo> svnFiles) {
		pendingWorkingCopyModifications.offer(svnFiles);

		// FIXME: mudar base lighhousefile para arquivo com merge

		// refreshModelBasedOnWorkingCopy(svnFiles); // acho q nao eh aki

		// FIXME: Fire model events to show in the LH view
	}

	private HashMap<String, Date> refreshWorkingCopy(
			Map<IFile, ISVNInfo> svnFiles) {
		for (Entry<IFile, ISVNInfo> entry : svnFiles.entrySet()) {
			String fqn = getClassFullyQualifiedName(entry.getKey());
			if (fqn != null) {
				ISVNInfo svnInfo = entry.getValue();
				mapClassFqnToLastRevisionTimestamp.put(fqn, svnInfo
						.getLastChangedDate());
			}
		}
		return mapClassFqnToLastRevisionTimestamp;
	}

	private List<String> getClassesFullyQualifiedName(
			Map<IFile, ISVNInfo> svnFiles) {
		LinkedList<String> result = new LinkedList<String>();
		for (IFile iFile : svnFiles.keySet()) {
			String fqn = getClassFullyQualifiedName(iFile);
			if (fqn != null) {
				result.add(fqn);
			}
		}
		return result;
	}

	private String getClassFullyQualifiedName(IFile iFile) {
		String result = null;
		try {
			ICompilationUnit icu = JavaCore.createCompilationUnitFrom(iFile);
			IType[] types = icu.getTypes();
			for (IType iType : types) {
				String fileNameWithoutExtension = iFile.getName().replaceAll(
						".java", "");
				String className = iType.getFullyQualifiedName().replaceAll(
						"\\w+\\.", "");
				if (fileNameWithoutExtension.equals(className)) {
					// Java files should have at least one class with the same
					// name of the file
					result = iType.getFullyQualifiedName();
					break;
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return result;
	}
	
	private void fireModificationsToUI(Collection<LighthouseEvent> events) {
		// We need hashmap to avoid repaint the UI multiple times
		HashMap<LighthouseClass, LighthouseEvent.TYPE> mapClassEvent = new HashMap<LighthouseClass, LighthouseEvent.TYPE>();
		HashMap<LighthouseRelationship, LighthouseEvent.TYPE> mapRelationshipEvent = new HashMap<LighthouseRelationship, LighthouseEvent.TYPE>();
		LighthouseModel model = LighthouseModel.getInstance();
		
		for (LighthouseEvent event : events) {
			Object artifact = event.getArtifact();

			// ADD creates a new class node in the view and populates it
			// MODIFY just re-populates the class node
			if (artifact instanceof LighthouseClass) {
				LighthouseClass klass = (LighthouseClass) artifact;
				mapClassEvent.put(klass, event.getType());
			} else if (artifact instanceof LighthouseEntity) {
				LighthouseModelManager manager = new LighthouseModelManager(model);
				LighthouseClass klass = manager.getMyClass((LighthouseEntity) artifact);
				if (klass != null) {
					// Never overwrite the ADD event
					if (!mapClassEvent.containsKey(klass)) {
						// Just refresh the class node
						mapClassEvent.put(klass, LighthouseEvent.TYPE.MODIFY);
					}
				}
			} else if (artifact instanceof LighthouseRelationship) {
				LighthouseRelationship relationship = (LighthouseRelationship) artifact;
				mapRelationshipEvent.put(relationship, event.getType());
			}
		}
		// Fire class changes to the UI
		for (Entry<LighthouseClass, TYPE> entry : mapClassEvent.entrySet()) {
			model.fireClassChanged(entry.getKey(),entry.getValue());
		}
		// Fire relationship changes to the UI
		for (Entry<LighthouseRelationship, TYPE> entry : mapRelationshipEvent.entrySet()) {
			model.fireRelationshipChanged(entry.getKey(),entry.getValue());
		}
	}

	@Override
	public void run() {
		threadRunning = true;

		while (threadRunning) {

			logger.debug("timeout()");
			refreshModelBasedOnLastDBAccess();

			// Sleep fot the time defined by thread timeout
			try {
				Thread.sleep(threadTimeout);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}
}
