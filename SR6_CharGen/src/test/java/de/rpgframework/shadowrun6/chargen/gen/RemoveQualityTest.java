package de.rpgframework.shadowrun6.chargen.gen;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rpgframework.genericrpg.data.Decision;
import de.rpgframework.genericrpg.items.CarriedItem;
import de.rpgframework.genericrpg.items.CarryMode;
import de.rpgframework.genericrpg.items.GearTool;
import de.rpgframework.shadowrun.chargen.charctrl.IMetatypeController;
import de.rpgframework.shadowrun6.SR6MetaType;
import de.rpgframework.shadowrun6.Shadowrun6Character;
import de.rpgframework.shadowrun6.Shadowrun6Core;
import de.rpgframework.shadowrun6.Shadowrun6Tools;
import de.rpgframework.shadowrun6.chargen.gen.karma.KarmaCharacterGenerator;
import de.rpgframework.shadowrun6.data.Shadowrun6DataPlugin;
import de.rpgframework.shadowrun6.items.ItemTemplate;

/**
 * COM6-932: itemmod type="QUALITY" remove="true" must not add the quality
 */
public class RemoveQualityTest {

	private final static UUID ORTHOSKIN_RATING = UUID.fromString("c2d17c87-1cfe-4355-9877-a20fe09c170d");

	//-------------------------------------------------------------------
	@BeforeClass
	public static void setupClass() {
		Locale.setDefault(Locale.ENGLISH);
		new Shadowrun6DataPlugin().init();
	}

	//-------------------------------------------------------------------
	@Test
	public void testOrthoskinDoesNotAddDermalDeposits() {
		Shadowrun6Character model = new Shadowrun6Character();
		model.addCarriedItem(buildOrthoskin(model));

		Shadowrun6Tools.runProcessors(model, Locale.getDefault());

		assertNull("dermal_deposits must not be added by orthoskin", model.getQuality("dermal_deposits"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testOrthoskinReplacesTrollDermalDeposits() throws Exception {
		Shadowrun6Character model = new Shadowrun6Character();
		model.setMetatype(Shadowrun6Core.getItem(SR6MetaType.class, "troll"));
		Shadowrun6Tools.runProcessors(model, Locale.getDefault());
		assertNotNull("Trolls should have dermal_deposits", model.getQuality("dermal_deposits"));

		model.addCarriedItem(buildOrthoskin(model));
		Shadowrun6Tools.runProcessors(model, Locale.getDefault());
		assertNull("dermal_deposits must be replaced by orthoskin", model.getQuality("dermal_deposits"));

		// After an XML round trip and plain loading
		Shadowrun6Character loaded = Shadowrun6Core.decode(Shadowrun6Core.encode(model));
		Shadowrun6Tools.resolveChar(loaded);
		Shadowrun6Tools.runProcessors(loaded, Locale.getDefault());
		assertNull("dermal_deposits must be replaced by orthoskin after loading", loaded.getQuality("dermal_deposits"));
	}

	//-------------------------------------------------------------------
	@Test
	public void testOrthoskinReplacesTrollDermalDepositsInGenerator() throws Exception {
		Shadowrun6Character model = new Shadowrun6Character();
		KarmaCharacterGenerator charGen = new KarmaCharacterGenerator();
		charGen.setModel(model, null);
		IMetatypeController<SR6MetaType> metaCtrl = charGen.getMetatypeController();
		metaCtrl.select(Shadowrun6Core.getItem(SR6MetaType.class, "troll"));
		charGen.runProcessors();
		assertNotNull("Trolls should have dermal_deposits", model.getQuality("dermal_deposits"));

		model.addCarriedItem(buildOrthoskin(model));
		charGen.runProcessors();
		assertNull("dermal_deposits must be replaced by orthoskin", model.getQuality("dermal_deposits"));

		// Reopen in generator
		Shadowrun6Character loaded = Shadowrun6Core.decode(Shadowrun6Core.encode(model));
		Shadowrun6Tools.resolveChar(loaded);
		KarmaCharacterGenerator reopened = new KarmaCharacterGenerator(loaded, null);
		reopened.setModel(loaded, null);
		reopened.runProcessors();
		assertNull("dermal_deposits must be replaced by orthoskin after reopening", loaded.getQuality("dermal_deposits"));
	}

	//-------------------------------------------------------------------
	private static CarriedItem<ItemTemplate> buildOrthoskin(Shadowrun6Character model) {
		ItemTemplate orthoskin = Shadowrun6Core.getItem(ItemTemplate.class, "orthoskin");
		assertNotNull(orthoskin);
		return GearTool.buildItem(orthoskin, CarryMode.IMPLANTED, model, true,
				new Decision(ORTHOSKIN_RATING, "2"),
				new Decision(ItemTemplate.CHOICE_AUGMENTATION_QUALITY, "STANDARD")).get();
	}

}
