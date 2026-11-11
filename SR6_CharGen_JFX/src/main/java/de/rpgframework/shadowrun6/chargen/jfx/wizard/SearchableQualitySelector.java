package de.rpgframework.shadowrun6.chargen.jfx.wizard;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import de.rpgframework.ResourceI18N;
import de.rpgframework.genericrpg.modification.Modification;
import de.rpgframework.genericrpg.requirements.Requirement;
import de.rpgframework.shadowrun.Quality;
import de.rpgframework.shadowrun.Quality.QualityType;
import de.rpgframework.shadowrun.chargen.charctrl.IQualityController;
import de.rpgframework.shadowrun.chargen.jfx.pages.FilterQualities;
import de.rpgframework.shadowrun.chargen.jfx.pane.QualitySelector;
import javafx.scene.control.TextField;

/**
 * Quality selector for the life path pages with an additional search field
 */
public class SearchableQualitySelector extends QualitySelector {

	private final static ResourceBundle RES = ResourceBundle.getBundle(SearchableQualitySelector.class.getPackageName()+".SR6WizardPages");

	private TextField tfSearch;

	//-------------------------------------------------------------------
	public SearchableQualitySelector(IQualityController ctrl, Function<Requirement, String> resolver,
			Function<Modification, String> mResolver, QualityType... allowed) {
		super(ctrl, resolver, mResolver, allowed);
		initSearch();
	}

	//-------------------------------------------------------------------
	public SearchableQualitySelector(IQualityController ctrl, Function<Requirement, String> resolver,
			Function<Modification, String> mResolver, FilterQualities.PosNeg posNeg, QualityType... allowed) {
		super(ctrl, resolver, mResolver, posNeg, allowed);
		initSearch();
	}

	//-------------------------------------------------------------------
	private void initSearch() {
		tfSearch = new TextField();
		tfSearch.setPromptText(ResourceI18N.get(RES, "page.quality.search.prompt"));
		tfSearch.textProperty().addListener( (ov,o,n) -> setBaseFilter(quality -> matches(quality, n)));
		col1.getChildren().add(0, tfSearch);
	}

	//-------------------------------------------------------------------
	private static boolean matches(Quality quality, String search) {
		if (search==null || search.isBlank())
			return true;
		String name = quality.getName(Locale.getDefault());
		return name!=null && name.toLowerCase(Locale.getDefault()).contains(search.trim().toLowerCase(Locale.getDefault()));
	}

}
