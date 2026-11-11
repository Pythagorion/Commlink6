package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.util.List;
import java.util.UUID;

import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.gen.SR6ContactGenerator;

/**
 * 
 */
public class SR6LifePathContactGenerator extends SR6ContactGenerator {

	//-------------------------------------------------------------------
	/**
	 * @param parent
	 */
	public SR6LifePathContactGenerator(SR6CharacterController parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		return super.process(previous);
	}

	//-------------------------------------------------------------------
	/**
	 * The connection from coming of age always has 4 points, independent of CHARISMA
	 * @see de.rpgframework.shadowrun6.chargen.gen.SR6ContactGenerator#isExemptFromCharismaLimit(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	protected boolean isExemptFromCharismaLimit(Contact contact) {
		if (!(parent instanceof SR6LifepathCharacterGenerator lifepath))
			return false;
		UUID id = lifepath.getSettings().getEarlyAdultContact();
		return id!=null && id.equals(contact.getId());
	}

}
