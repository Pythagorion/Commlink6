package de.rpgframework.shadowrun6.chargen.gen;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.rpgframework.genericrpg.Possible;
import de.rpgframework.genericrpg.ToDoElement;
import de.rpgframework.genericrpg.ToDoElement.Severity;
import de.rpgframework.genericrpg.chargen.OperationResult;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.random.VariableHolderNode;
import de.rpgframework.shadowrun.Contact;
import de.rpgframework.shadowrun.ContactType;
import de.rpgframework.shadowrun.ShadowrunAttribute;
import de.rpgframework.shadowrun.chargen.charctrl.IRejectReasons;
import de.rpgframework.shadowrun.generators.ShadowrunNameGenerator;
import de.rpgframework.shadowrun6.CreatePoints;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Rules;
import de.rpgframework.shadowrun6.chargen.charctrl.ControllerImpl;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6CharacterController;
import de.rpgframework.shadowrun6.chargen.charctrl.SR6ContactController;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

/**
 * @author prelle
 *
 */
public class SR6ContactGenerator extends ControllerImpl<Contact> implements SR6ContactController {

	protected static Logger logger = System.getLogger(ControllerImpl.class.getPackageName()+".contact");

	private static Random random = new Random();

	private ShadowrunNameGenerator nameGen;

	protected int pointsLeft;

	//-------------------------------------------------------------------
	public SR6ContactGenerator(SR6CharacterController parent) {
		super(parent);
		nameGen = new ShadowrunNameGenerator();
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#getPointsLeft()
	 */
	@Override
	public int getPointsLeft() {
		return pointsLeft;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#canCreateContact()
	 */
	@Override
	public Possible canCreateContact() {
		return new Possible(pointsLeft>=2);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#createContact()
	 */
	@Override
	public OperationResult<Contact> createContact() {
		Possible poss = canCreateContact();
		if (!poss.get()) {
			logger.log(Level.ERROR, "Trying to create a contact, which is not allowed");
			return new OperationResult<>(poss);
		}

		Contact contact = new Contact();
		contact.setType(ContactType.STREET);
		getModel().addContact(contact);
		logger.log(Level.INFO, "Added contact");

		parent.runProcessors();

		return new OperationResult<Contact>(contact);
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#removeContact(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public void removeContact(Contact con) {
		boolean success = getModel().removeContact(con);
		logger.log(Level.INFO, "Removed contact");
		if (success) {
			parent.runProcessors();
		}
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#canIncreaseRating(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public Possible canIncreaseRating(Contact con) {
		if (con==null) return Possible.FALSE;
		// No matter what rules: Cannot be higher than 8
		if (con.getRating()>=8) {
			return new Possible(IRejectReasons.IMPOSS_MAX_LEVEL_REACHED);
		}
		Shadowrun6Character model = getModel();

		if (parent.getRuleController().getRuleValueAsBoolean(Shadowrun6Rules.CHARGEN_EXTENDED_CONTACT)) {
			int karmaNeeded = 0;
			// Apply extended contact rules
			int sum = con.getLoyalty() + con.getRating();
			// SSDR on 01.06.2022: "I would sayit does, an augmented bonus to an attribute counts as that attribute in essentially every way, except karma cost for advancing"
			int maxWithoutKarma = model.getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue() * 2;
			// If the sum is already at normal max, you need Karma to increase the maximum
			if (sum>=maxWithoutKarma) {
				if (model.getKarmaFree()<1)
					return new Possible(Severity.STOPPER, IRejectReasons.RES, IRejectReasons.IMPOSS_NOT_ENOUGH_KARMA, 1);
				karmaNeeded = 1;
			}
			// Either max not reached or max can be increased
			if (pointsLeft==0 && model.getKarmaFree()<(1+karmaNeeded)) {
				return new Possible(IRejectReasons.IMPOSS_NOT_ENOUGH_POINTS);
			}

		} else {
			// Core rules
			int max = Math.min(8, getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue());
			if (con.getRating()>=max)
				return new Possible(IRejectReasons.IMPOSS_MAX_LEVEL_REACHED);
			if (pointsLeft<1)
				return new Possible(IRejectReasons.IMPOSS_NOT_ENOUGH_POINTS);
		}
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#increaseRating(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public boolean increaseRating(Contact con) {
		Possible poss = canIncreaseRating(con);
		if (!poss.get()) {
			logger.log(Level.ERROR, "Tried to increase contact rating although not possible: "+poss.getMostSevere());
			return false;
		}

		con.setRating(con.getRating()+1);
		logger.log(Level.INFO, "Increased contact rating of "+con.getName());

		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#canDecreaseRating(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public Possible canDecreaseRating(Contact con) {
		if (con==null) return Possible.FALSE;
		if (con.getRating()<=1)
			return new Possible(IRejectReasons.IMPOSS_MIN_LEVEL_REACHED);
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#decreaseRating(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public boolean decreaseRating(Contact con) {
		if (!canDecreaseRating(con).get()) {
			logger.log(Level.ERROR, "Tried to decrease contact rating although not possible");
			return false;
		}

		con.setRating(con.getRating()-1);
		logger.log(Level.INFO, "Decreased contact rating of {0} to {1}",con.getName(), con.getRating());

		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#canIncreaseLoyalty(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public Possible canIncreaseLoyalty(Contact con) {
		if (con==null) return Possible.FALSE;
		// No matter what rules: Cannot be higher than 8
		if (con.getLoyalty()>=8) {
			return new Possible(IRejectReasons.IMPOSS_MAX_LEVEL_REACHED);
		}
		Shadowrun6Character model = getModel();

		if (parent.getRuleController().getRuleValueAsBoolean(Shadowrun6Rules.CHARGEN_EXTENDED_CONTACT)) {
			int karmaNeeded = 0;
			// Apply extended contact rules
			int sum = con.getLoyalty() + con.getRating();
			// SSDR on 01.06.2022: "I would say it does, an augmented bonus to an attribute counts as that attribute in essentially every way, except karma cost for advancing"
			int maxWithoutKarma = model.getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue() * 2;
			// If the sum is already at normal max, you need Karma to increase the maximum
			if (sum>=maxWithoutKarma) {
				if (model.getKarmaFree()<1)
					return new Possible(Severity.STOPPER, IRejectReasons.RES, IRejectReasons.IMPOSS_NOT_ENOUGH_KARMA, 1);
				karmaNeeded = 1;
			}
			// Either max not reached or max can be increased
			if (pointsLeft==0 && model.getKarmaFree()<(1+karmaNeeded)) {
				return new Possible(IRejectReasons.IMPOSS_NOT_ENOUGH_POINTS);
			}

		} else {
			// Core rules
			int max = Math.min(8, getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue());
			if (con.getLoyalty()>=max)
				return new Possible(IRejectReasons.IMPOSS_MAX_LEVEL_REACHED);
			if (pointsLeft<1)
				return new Possible(IRejectReasons.IMPOSS_NOT_ENOUGH_POINTS);
		}
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#increaseLoyalty(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public boolean increaseLoyalty(Contact con) {
		Possible poss = canIncreaseLoyalty(con);
		if (!poss.get()) {
			logger.log(Level.ERROR, "Tried to increase contact loyalty although not possible: "+poss.getMostSevere());
			return false;
		}

		con.setLoyalty(con.getLoyalty()+1);
		logger.log(Level.INFO, "Increased contact loyalty of "+con.getName());

		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#canDecreaseLoyalty(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public Possible canDecreaseLoyalty(Contact con) {
		if (con==null) return Possible.FALSE;
		if (con.getLoyalty()<=1)
			return new Possible(IRejectReasons.IMPOSS_MIN_LEVEL_REACHED);
		return Possible.TRUE;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.shadowrun.chargen.charctrl.IContactController#decreaseLoyalty(de.rpgframework.shadowrun.Contact)
	 */
	@Override
	public boolean decreaseLoyalty(Contact con) {
		if (!canDecreaseLoyalty(con).get()) {
			logger.log(Level.ERROR, "Tried to decrease contact loyalty although not possible");
			return false;
		}

		con.setLoyalty(con.getLoyalty()-1);
		logger.log(Level.INFO, "Decreased contact loyalty of "+con.getName());

		parent.runProcessors();
		return true;
	}

	//-------------------------------------------------------------------
	/**
	 * @see de.rpgframework.character.ProcessingStep#process(java.util.List)
	 */
	@Override
	public List<Modification> process(List<Modification> previous) {
		if (logger.isLoggable(Level.TRACE)) logger.log(Level.TRACE, "ENTER process");
		List<Modification> unprocessed = new ArrayList<>();
		try {
			todos.clear();

			int contactPoints = 0;
			for (Modification tmp : previous) {
				if (tmp instanceof ValueModification && tmp.getReferenceType()==ShadowrunReference.CREATION_POINTS && ((ValueModification)tmp).getResolvedKey()==CreatePoints.CONTACT_POINTS) {
					contactPoints += ((ValueModification)tmp).getValue();
				} else {
					unprocessed.add(tmp);
				}
			}

			// Calculate points left
			pointsLeft = (contactPoints>0)?contactPoints:(getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue() * 6);
			logger.log(Level.INFO, "Have {0} points to spend on contacts", pointsLeft);

			// Now pay contacts
			int karmaRequired = 0;
			int perContactMax = getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue()*2;
			for (Contact tmp : getModel().getContacts()) {
				// is sum higher than allowed
				if ((tmp.getLoyalty()+tmp.getRating())>perContactMax && !isExemptFromCharismaLimit(tmp)) {
					if (parent.getRuleController().getRuleValueAsBoolean(Shadowrun6Rules.CHARGEN_EXTENDED_CONTACT)) {
						int pay = (tmp.getLoyalty() + tmp.getRating()) - perContactMax;
						logger.log(Level.INFO, "Pay {0} karma (for cap increase of (R={2}/L={3})", pay,
								tmp.getName(), tmp.getRating(), tmp.getLoyalty());
						karmaRequired += pay;
					} else {
						tmp.setRating(Math.min(getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue(), tmp.getRating()));
						tmp.setLoyalty(Math.min(getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue(), tmp.getLoyalty()));
						logger.log(Level.INFO, "Cap values of {0} to (R={1}/L={2})", tmp.getName(), tmp.getRating(), tmp.getLoyalty());
					}
				}
				int cost = tmp.getLoyalty() + tmp.getRating();
				logger.log(Level.INFO, "Pay {0} contact points {1} (R={2}/L={3})", cost, tmp.getName(),tmp.getRating(), tmp.getLoyalty());
				pointsLeft -= cost;
			}

			if (pointsLeft<0 && parent.getRuleController().getRuleValueAsBoolean(Shadowrun6Rules.CHARGEN_EXTENDED_CONTACT)) {
				logger.log(Level.INFO, "Pay {0} karma for more contact points", Math.abs(pointsLeft));
				karmaRequired += Math.abs(pointsLeft);
				pointsLeft=0;
			}
			// Pay Karma
			if (karmaRequired>0) {
				logger.log(Level.WARNING, "Need {0} karma to pay for contacts", karmaRequired);
			}
			getModel().setKarmaFree( getModel().getKarmaFree() - karmaRequired );

			logger.log(Level.INFO, "Contact points remaining: {0}", pointsLeft);
			if (pointsLeft>0) {
				todos.add(new ToDoElement(Severity.WARNING, IRejectReasons.RES, IRejectReasons.TODO_CONTACT_POINTS_LEFT, pointsLeft));
			}
			if (pointsLeft<0) {
				todos.add(new ToDoElement(Severity.STOPPER, IRejectReasons.RES, IRejectReasons.TODO_TOO_MANY_CONTACT_POINTS, Math.abs(pointsLeft)));
			}

		} finally {
			if (logger.isLoggable(Level.TRACE)) logger.log(Level.TRACE, "LEAVE process");
		}
		return unprocessed;
	}

	//-------------------------------------------------------------------
	/**
	 * @return TRUE for contacts whose values are fixed by the rules and must not be capped by CHARISMA
	 */
	protected boolean isExemptFromCharismaLimit(Contact contact) {
		return false;
	}

	//-------------------------------------------------------------------
	@Override
	public void roll() {
		logger.log(Level.ERROR, "roll() not implemented");

		// SSDR on 01.06.2022: "I would say it does, an augmented bonus to an attribute counts as that attribute in essentially every way, except karma cost for advancing"
		int max = getModel().getAttribute(ShadowrunAttribute.CHARISMA).getModifiedValue();
		while (pointsLeft>1) {
			int rating = (pointsLeft==2)?1:(1+random.nextInt(Math.min(max, pointsLeft-1)));
			pointsLeft -= rating;
			int loyalty = (pointsLeft==1)?1:(1+random.nextInt(1+ Math.min(max, pointsLeft)));
			pointsLeft -= loyalty;
			Contact contact = new Contact();
			contact.setLoyalty(loyalty);
			contact.setRating(rating);
			int type = random.nextInt(Shadowrun6Core.getItemList(ContactType.class).size());
			contact.setType(Shadowrun6Core.getItemList(ContactType.class).get(type).getId());

			// Generate a name
			String name = (String) nameGen.generate(new VariableHolderNode());
			contact.setName(name);

			logger.log(Level.ERROR, "Generated {0}: {1}", name, contact);
			getModel().addContact(contact);
			logger.log(Level.INFO, "Added contact");
		}
	}

}
