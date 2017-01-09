package com.google.glass.glassware.zoomin;

import java.util.ArrayList;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;

/**
 * CardScrollAdapter will the display the timeline in the form of a Card Scroll Adapter view.
 */
class GlassCardScrollAdapter extends CardScrollAdapter {

	private List<Card>		    mCards;

	public GlassCardScrollAdapter() {

		mCards = new ArrayList<Card>();
	}

	@Override
	public int getPosition(Object item) {
		return mCards.indexOf(item);
	}

	@Override
	public int getCount() {
		return mCards.size();
	}

	@Override
	public Object getItem(int position) {
		return mCards.get(position);
	}

	/**
	 * Returns the amount of view types.
	 */
	@Override
	public int getViewTypeCount() {
		return Card.getViewTypeCount();
	}

	/**
	 * Returns the view type of this card so the system can figure out
	 * if it can be recycled.
	 */
	@Override
	public int getItemViewType(int position){
		return mCards.get(position).getItemViewType();
	}

	@Override
	public View getView(int position, View convertView,
			ViewGroup parent) {
		return  mCards.get(position).getView(convertView, parent);
	}

	public List<Card> getCards() {
		return mCards;
	}
}
