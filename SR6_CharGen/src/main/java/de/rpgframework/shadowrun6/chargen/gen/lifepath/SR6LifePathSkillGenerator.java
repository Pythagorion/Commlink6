package de.rpgframework.shadowrun6.chargen.gen.lifepath;

import java.util.ArrayList;
import java.util.List;

import de.rpgframework.character.ProcessingStep;
import de.rpgframework.genericrpg.ValueType;
import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.modification.DataItemModification;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.modification.ValueModification;
import de.rpgframework.shadowrun6.SR6Skill;
import de.rpgframework.shadowrun6.SR6SkillValue;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.modifications.ShadowrunReference;

public class SR6LifePathSkillGenerator implements ProcessingStep {

	private final Shadowrun6Character model;

	//-------------------------------------------------------------------
	public SR6LifePathSkillGenerator(Shadowrun6Character model) {
		this.model = model;
	}

	//-------------------------------------------------------------------
	@Override
	public List<Modification> process(List<Modification> previous) {
		List<Modification> unprocessed = new ArrayList<>();

		for (Modification mod : previous) {
			if (mod instanceof ValueModification && mod.getReferenceType()==ShadowrunReference.SKILL) {
				applySkill((ValueModification) mod);
			} else if (mod instanceof DataItemModification && mod.getReferenceType()==ShadowrunReference.SKILL) {
				addSkill((DataItemModification) mod);
			} else {
				unprocessed.add(mod);
			}
		}

		return unprocessed;
	}

	//-------------------------------------------------------------------
	private void applySkill(ValueModification mod) {
		SR6Skill skill = mod.getReferenceType().resolve(mod.getKey());
		if (skill==null)
			return;

		SR6SkillValue value = model.getSkillValue(skill);
		if (value==null) {
			value = new SR6SkillValue(skill, 0);
			model.addSkillValue(value);
		}
		for (Decision dec : mod.getDecisions()) {
			value.addDecision(dec);
		}

		ValueModification natural = new ValueModification(ShadowrunReference.SKILL, skill.getId(), mod.getValue(), mod.getSource(), ValueType.NATURAL);
		natural.getDecisions().addAll(mod.getDecisions());
		value.addIncomingModification(natural);
	}

	//-------------------------------------------------------------------
	/**
	 * An &lt;itemmod&gt; grants an additional skill (e.g. a named knowledge skill) at rating 1
	 */
	private void addSkill(DataItemModification mod) {
		SR6Skill skill = mod.getReferenceType().resolve(mod.getKey());
		if (skill==null)
			return;

		SR6SkillValue value = new SR6SkillValue(skill, 0);
		if (mod.getId()!=null)
			value.setUuid(mod.getId());
		for (Decision dec : mod.getDecisions()) {
			value.addDecision(dec);
		}
		model.addSkillValue(value);

		ValueModification natural = new ValueModification(ShadowrunReference.SKILL, skill.getId(), 1, mod.getSource(), ValueType.NATURAL);
		natural.getDecisions().addAll(mod.getDecisions());
		value.addIncomingModification(natural);
	}

}
