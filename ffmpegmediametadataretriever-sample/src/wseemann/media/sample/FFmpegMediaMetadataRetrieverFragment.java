/*
 * FFmpegMediaMetadataRetriever: A unified interface for retrieving frame 
 * and meta data from an input media file.
 *
 * Copyright 2013 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wseemann.media.sample;

import java.util.List;

import android.os.Bundle;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

public class FFmpegMediaMetadataRetrieverFragment extends ListFragment
		implements LoaderManager.LoaderCallbacks<List<Metadata>> {

	private int mId = 0;
	private ImageView mImage;
	
	// This is the Adapter being used to display the list's data.
    private MetadataListAdapter mAdapter;
	
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRetainInstance(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View layout = super.onCreateView(inflater, container, savedInstanceState);
    	ListView lv = (ListView) layout.findViewById(android.R.id.list);
    	ViewGroup parent = (ViewGroup) lv.getParent();
    	
    	View v = inflater.inflate(R.layout.fragment, container, false);
    	
    	// Remove ListView and add my view in its place
        int lvIndex = parent.indexOfChild(lv);
        parent.removeViewAt(lvIndex);
        parent.addView(v, lvIndex, lv.getLayoutParams());
    	
    	final EditText uriText = (EditText) v.findViewById(R.id.uri);
    	// Uncomment for debugging
    	//uriText.setText("http://...");
    	
    	Button goButton = (Button) v.findViewById(R.id.go_button);
    	goButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Start out with a progress indicator.
				setListShown(false);
				
				String uriString = uriText.getText().toString();
				
				Bundle bundle = new Bundle();
				bundle.putString("uri", uriString);
				
				mId++;
				FFmpegMediaMetadataRetrieverFragment.this.getLoaderManager().initLoader(mId, bundle, FFmpegMediaMetadataRetrieverFragment.this);
			}
		});
    	
    	return layout;
    }
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data.  In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.no_metadata));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        if (mAdapter == null) {
        	View header = getLayoutInflater(savedInstanceState).inflate(R.layout.header, null);
        	mImage = (ImageView) header.findViewById(R.id.image);
        	
        	getListView().addHeaderView(header);
        	
        	// Create an empty adapter we will use to display the loaded data.
        	mAdapter = new MetadataListAdapter(getActivity());
        	setListAdapter(mAdapter);
        } else {
        	// Start out with a progress indicator.
			setListShown(false);
        	
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(mId, new Bundle(), this);
        }
    }
	
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(
				R.menu.ffmpeg_media_metadata_retriever_sample, menu);
		return true;
	}*/

	@Override
	public Loader<List<Metadata>> onCreateLoader(int arg0, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with one argument, so it is simple.
		return new MetadataLoader(getActivity(), args);
	}

	@Override
	public void onLoadFinished(Loader<List<Metadata>> loader, List<Metadata> metadata) {
		Bitmap b = null;
		int imageIndex = -1;
		
		mImage.setImageResource(0);
		
		for (int i = 0; i < metadata.size(); i++) {
			if (metadata.get(i).getKey().equals("image")) {
				imageIndex = i;
				
				b = (Bitmap) metadata.get(i).getValue();
				if (b != null) {
					float density = getResources().getDisplayMetrics().density;
					int scale = (int) (200 * density);
					Bitmap bm = Bitmap.createScaledBitmap(b, scale, scale, true);
					mImage.setImageBitmap(bm);
				}
			}
		}
		
		if (imageIndex != -1) {
			metadata.remove(imageIndex);
		}
		
        // Set the new metadata in the adapter.
        mAdapter.setMetadata(metadata);

        if (b != null) {
        	metadata.add(new Metadata("image", b));
        }
        
        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
	}

	@Override
	public void onLoaderReset(Loader<List<Metadata>> metadata) {
        // Clear the metadata in the adapter.
        mAdapter.setMetadata(null);
	}
	
	private static class MetadataListAdapter extends ArrayAdapter<Metadata> {
	    private final LayoutInflater mInflater;

	    public MetadataListAdapter(Context context) {
	        super(context, android.R.layout.simple_list_item_2);
	        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }

	    public void setMetadata(List<Metadata> metadata) {
	        clear();
	        if (metadata != null) {
	        	for (int i = 0; i < metadata.size(); i++) {
	        		add(metadata.get(i));
	        	}
	        }
	    }

	    /**
	     * Populate new items in the list.
	     */
	    @Override public View getView(int position, View convertView, ViewGroup parent) {
	        View view;

	        if (convertView == null) {
	            view = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
	        } else {
	            view = convertView;
	        }

	        Metadata item = getItem(position);
	        ((TextView)view.findViewById(android.R.id.text2)).setText(item.getKey());
	        ((TextView)view.findViewById(android.R.id.text1)).setText((String) item.getValue());

	        return view;
	    }
	}
}
