package com.example.ankumar.sdcardscanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ankumar on 5/14/16.
 */
public class ListAdapter<T extends ArrayList> extends ArrayAdapter {

	T items = (T) new ArrayList<>();
	Context context;
	int resource;
	int id;

	public ListAdapter(Context context, int resource, int id) {
		super(context, resource,id);
		this.context = context;
		this.resource = resource;
		this.id = id;
	}

	static class ViewHolder {
		public TextView firstText;
		public TextView secondText;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if(rowView == null) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(resource, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.firstText = (TextView) rowView.findViewById(R.id.firstText);
			viewHolder.secondText = (TextView) rowView.findViewById(R.id.secondText);
			rowView.setTag(viewHolder);
		}

		Object item = items.get(position);
		ViewHolder holder = (ViewHolder) rowView.getTag();
		if(item instanceof FileInfo) {
			holder.firstText.setText(((FileInfo)item).getPath());
			holder.secondText.setText(String.valueOf(((FileInfo)item).getSize()));
		} else if (item instanceof FileTypeFrequency) {
			holder.firstText.setText(((FileTypeFrequency)item).getFileExtension());
			holder.secondText.setText(String.valueOf(((FileTypeFrequency)item).getFrequency()));
		}

		return rowView;
	}

	@Override
	public void addAll(Collection collection) {
		items.addAll(collection);
	}

	@Override
	public void clear() {
		items.clear();
		notifyDataSetChanged();
		super.clear();
	}

	@Override
	public int getCount() {
		if(items != null) {
			return items.size();
		} else {
			return 0;
		}
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}
}
