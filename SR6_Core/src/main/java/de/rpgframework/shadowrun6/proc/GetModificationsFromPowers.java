package de.rpgframework.shadowrun6.proc;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import de.rpgframework.character.ProcessingStep;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.shadowrun.AdeptPower;
import de.rpgframework.shadowrun.AdeptPower.Activation;
import de.rpgframework.shadowrun.AdeptPowerValue;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Tools;

/**
 * @author prelle
 *
 */
public class GetModificationsFromPowers implements ProcessingStep {

	protected static final Logger logger = System.getLogger(GetModificationsFromPowers.class.getPackageName()+".adeptpower");

	private Shadowrun6Character model;

	//-------------------------------------------------------------------
	public GetModificationsFromPowers(Shadowrun6Character model) {
		this.model = model;
	}

	//-------------------------------------------------------------------
	/**
	 * @see org.prelle.shadowrun6.proc.CharacterProcessor#process(org.prelle.shadowrun5.ShadowrunCharacter, java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		List<Modification> unprocessed = new ArrayList<>(previous);

		logger.log(Level.TRACE,  "START: process");
		try {
			// Calculate effective modifications from available powers
			for (AdeptPowerValue ref :model.getAdeptPowers()) {
				AdeptPower power = ref.getModifyable();
				logger.log(Level.DEBUG, "add from power "+power.getId()+" / "+ref+" / dec="+ref.getDecisions());
				logger.log(Level.DEBUG, "ToDo: "+power.getOutgoingModifications());
				// Calculate modifications
				ref.clearOutgoingModifications();

				int multiplier = ref.getModifiedValue(); //(ref.getModifyable().getMaxLevel()>1)?ref.getModifiedValue():1;
//				if (multiplier==0) {
//					logger.log(Level.WARNING,"Strange! Found AdeptPower {0} with value 0 that should have at least 1 - ignore it: "+ref, ref.getKey());
////					continue;
//				}
				for (Modification mod : power.getOutgoingModifications()) {
					try {
						Modification realMod = Shadowrun6Tools.instantiateModification(mod, ref, multiplier, model);
						logger.log(Level.DEBUG, " instantiated "+realMod);
						if (ref.getModifyable().getActivation()!=Activation.PASSIVE) {
							if (realMod instanceof DataItemModification)
								((DataItemModification)realMod).setConditionString("GetModificationsFromPowers");
						}
						ref.addOutgoingModification(realMod);
						unprocessed.add(realMod);
					} catch (Exception e) {
						logger.log(Level.ERROR, "Problem calculating modifications for adept power: "+ref,e);
					}
				}
				// Incoming modifications (e.g. levels from mentor spirits) have
				// already been applied - don't emit them again
			}
		} finally {
			logger.log(Level.TRACE,  "STOP : process() ends with "+unprocessed.size()+" modifications still to process");
		}
		return unprocessed;
	}

}
