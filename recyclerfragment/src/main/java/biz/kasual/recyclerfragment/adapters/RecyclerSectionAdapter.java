package biz.kasual.recyclerfragment.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import biz.kasual.recyclerfragment.R;
import biz.kasual.recyclerfragment.views.DefaultViewHolder;

/**
 * Created by Stephen Vinouze on 09/11/2015.
 */
public class RecyclerSectionAdapter<T> extends RecyclerView.Adapter<DefaultViewHolder> {

    private static final int SECTION_TYPE = 0;

    protected Context mContext;
    private boolean mValid = true;
    private String mSortMethodName;
    private RecyclerAdapter<T> mBaseAdapter;
    private SparseArray<Section> mSections = new SparseArray<>();

    public RecyclerSectionAdapter(Context context) {
        mContext = context;
    }

    public void setBaseAdapter(RecyclerAdapter<T> baseAdapter) {
        mBaseAdapter = baseAdapter;
        mBaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateSections();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mValid = mBaseAdapter.getItemCount() > 0;
                notifyItemRangeChanged(positionToSectionPosition(positionStart), itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateSections();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateSections();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                mValid = mBaseAdapter.getItemCount() > 0;
                notifyItemMoved(positionToSectionPosition(fromPosition), positionToSectionPosition(toPosition));
            }
        });
    }

    public void setSortMethodName(String sortMethodName) {
        mSortMethodName = sortMethodName;
    }

    public String titleForSection(int sectionPosition) {
        return null;
    }

    public View onCreateSectionItemView(ViewGroup parent, int viewType) {
        return null;
    }

    public void onBindSectionItemView(View v, int sectionPosition) {}

    @Override
    public int getItemViewType(int position) {
        return isSectionAt(position) ? SECTION_TYPE : mBaseAdapter.getItemViewType(sectionPositionToPosition(position)) + 1;
    }

    @Override
    public long getItemId(int position) {
        return isSectionAt(position) ? Integer.MAX_VALUE - mSections.indexOfKey(position) : mBaseAdapter.getItemId(sectionPositionToPosition(position));
    }

    @Override
    public int getItemCount() {
        return (mValid ? mBaseAdapter.getItemCount() + mSections.size() : 0);
    }

    @Override
    public DefaultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == SECTION_TYPE) {
            View customView = onCreateSectionItemView(parent, viewType);
            if (customView != null && titleForSection(0) == null) {
                return new DefaultViewHolder(customView);
            }
            else {
                final View view = LayoutInflater.from(mContext).inflate(R.layout.section_view, parent, false);
                return new SectionDefaultViewHolder(view, R.id.section_text);
            }
        }
        else {
            return mBaseAdapter.onCreateViewHolder(parent, viewType - 1);
        }
    }

    @Override
    public void onBindViewHolder(DefaultViewHolder holder, int position) {
        if (isSectionAt(position)) {
            int sectionPosition = mSections.indexOfKey(position);
            if (holder instanceof SectionDefaultViewHolder) {
                TextView sectionTextView = ((SectionDefaultViewHolder)holder).title;
                String customTitle = titleForSection(sectionPosition);
                sectionTextView.setText(customTitle != null ? customTitle : mSections.get(position).getTitle());
            }
            else {
                onBindSectionItemView(holder.getView(), sectionPosition);
            }
        }
        else {
            mBaseAdapter.onBindViewHolder(holder, sectionPositionToPosition(position));
        }
    }

    public boolean isSectionAt(int position) {
        return mSections.get(position) != null;
    }

    public T getFirstItemInSection(int sectionPosition) {
        if (sectionPosition < mSections.size()) {
            int computedPosition = sectionPositionToPosition(mSections.keyAt(sectionPosition) + 1);
            return mBaseAdapter.getItemAt(computedPosition);
        }
        return null;
    }

    public int positionToSectionPosition(int position) {
        int offset = 0;
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.valueAt(i).firstPosition > position) {
                break;
            }
            ++offset;
        }
        return position + offset;
    }

    public int sectionPositionToPosition(int sectionPosition) {
        if (isSectionAt(sectionPosition)) {
            return RecyclerView.NO_POSITION;
        }

        int offset = 0;
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.valueAt(i).position > sectionPosition) {
                break;
            }
            --offset;
        }
        return sectionPosition + offset;
    }

    private void buildSections(List<T> items) {
        if (items.size() > 0) {
            try {
                final Method method = items.get(0).getClass().getMethod(mSortMethodName);
                if (method != null) {

                    List<Section> sections = new ArrayList<>();
                    String previousTitle = "";
                    for (T item : items) {
                        try {
                            String currentTitle = method.invoke(item).toString();
                            if (!currentTitle.equals(previousTitle)) {
                                sections.add(new Section(items.indexOf(item), currentTitle));
                                previousTitle = currentTitle;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    //Add your adapter to the sectionAdapter
                    setSections(sections.toArray(new Section[sections.size()]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setSections(Section[] sections) {
        mSections.clear();

        Arrays.sort(sections, new Comparator<Section>() {
            @Override
            public int compare(Section o, Section o1) {
                return (o.firstPosition == o1.firstPosition)
                        ? 0
                        : ((o.firstPosition < o1.firstPosition) ? -1 : 1);
            }
        });

        int offset = 0; // offset positions for the headers we're adding
        for (Section section : sections) {
            section.position = section.firstPosition + offset;
            mSections.append(section.position, section);
            ++offset;
        }

        notifyDataSetChanged();
    }

    private void updateSections() {
        mValid = mBaseAdapter.getItemCount() > 0;
        buildSections(mBaseAdapter.getItems());
    }

    public static class Section {

        private int firstPosition;
        private int position;
        private CharSequence title;

        public Section(int firstPosition, CharSequence title) {
            this.firstPosition = firstPosition;
            this.title = title;
        }

        public CharSequence getTitle() {
            return title;
        }
    }

    public static class SectionDefaultViewHolder extends DefaultViewHolder {

        public TextView title;

        public SectionDefaultViewHolder(View view, int mTextResourceid) {
            super(view);
            title = (TextView) view.findViewById(mTextResourceid);
        }
    }

}