package com.example.xdyblaster.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.xdyblaster.Adapter.DetonatorAdapter;
import com.example.xdyblaster.R;
import com.example.xdyblaster.util.DataViewModel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import utils.SerialPortUtils;

public class FragmentData extends Fragment {

    int rowNum;
    private LinearLayoutManager layoutManager;
    public DetonatorAdapter detonatorAdapter;
    private DataViewModel dataViewModel;
    private int mIndex;
    private boolean move;
    @BindView(R.id.rvDetonator)
    RecyclerView rvDetonator;
    Unbinder unbinder;
    SerialPortUtils serialPortUtils;
    public DetonatorAdapter.OnItemSelectedListener onItemSelectedListener = null;
    public boolean clickEn=true;

    public void setItemClickListener(DetonatorAdapter.OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        serialPortUtils = SerialPortUtils.getInstance(getActivity());
        dataViewModel = new ViewModelProvider(serialPortUtils.mActivity).get(DataViewModel.class);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_data, container, false);
        unbinder = ButterKnife.bind(this, root);
        Bundle bundle = getArguments();
        rowNum = bundle.getInt("rowNum");
        clickEn = bundle.getBoolean("click");

        detonatorAdapter = new DetonatorAdapter(getActivity(), dataViewModel.detonatorList.get(rowNum), dataViewModel);
        detonatorAdapter.clickEnable(clickEn);
        detonatorAdapter.onItemSelectedListener = onItemSelectedListener;
        detonatorAdapter.onScrollListener = new DetonatorAdapter.OnScrollListener() {
            @Override
            public void OnItemScroll(int item) {
//                int firstItem;
//                if (item == layoutManager.findLastVisibleItemPosition()) {
//                    firstItem = layoutManager.findFirstVisibleItemPosition();
//                    moveToPosition(firstItem + 2);
//                }
            }
        };
        layoutManager = new LinearLayoutManager(getActivity());
        rvDetonator.setLayoutManager(layoutManager);
        rvDetonator.setAdapter(detonatorAdapter);
        rvDetonator.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //???????????????????????????????????????????????????
//                if(move==false)
//                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                if (move) {
                    move = false;
                    //????????????????????????????????????????????????mIndex???????????????????????????RecyclerView????????????
                    int n = mIndex - layoutManager.findFirstVisibleItemPosition();
                    if (0 <= n && n < rvDetonator.getChildCount()) {
                        //??????????????????????????????RecyclerView???????????????
                        int top = rvDetonator.getChildAt(n).getTop();
                        //???????????????
                        rvDetonator.scrollBy(0, top);
                    }
                }
            }
        });


        dataViewModel.updateList.get(rowNum).observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                switch (integer) {
                    case -1:
                        detonatorAdapter.notifyDataSetChanged();
                        break;
                    case -2:
                        detonatorAdapter.notifyDataSetChanged();
                        moveToPosition(layoutManager.findLastVisibleItemPosition());
                        break;
                    case -3:
                        int firstItem = layoutManager.findFirstVisibleItemPosition();
                        int lastItem = layoutManager.findLastVisibleItemPosition();
                        int d = lastItem - firstItem;
                        d = firstItem - d;
                        if (d < 0)
                            d = 0;
                        moveToPosition(d);
                        break;
                    case -4:
                        firstItem = layoutManager.findFirstVisibleItemPosition();
                        d = firstItem - 1;
                        if (d < 0)
                            d = 0;
                        moveToPosition(d);
                        break;
                    case -5:
                        detonatorAdapter.notifyDataSetChanged();
                        firstItem = layoutManager.findFirstVisibleItemPosition();
                        d = firstItem + 1;
                        if (d < dataViewModel.detonatorList.get(rowNum).size())
                            moveToPosition(d);
                        break;
                    default:
                        moveToPosition(integer);
                        detonatorAdapter.selection = integer;
                        detonatorAdapter.opened = -1;
                        detonatorAdapter.notifyDataSetChanged();
                        break;
                }

            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void moveToPosition(int i) {
        //????????????recycleView??????????????????????????????????????????Position
        // behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        int firstItem = layoutManager.findFirstVisibleItemPosition();
        int lastItem = layoutManager.findLastVisibleItemPosition();
        mIndex = i;
        //??????????????????
        if (i <= firstItem) {
            //????????????????????????????????????????????????????????????
            rvDetonator.scrollToPosition(i);
        } else if (i <= lastItem) {
            //?????????????????????????????????????????????????????????????????????????????????
            int top = rvDetonator.getChildAt(i - firstItem).getTop();
            rvDetonator.scrollBy(0, top);
        } else {
            //????????????????????????????????????????????????????????????
            rvDetonator.scrollToPosition(i);
            //?????????????????????RecyclerView???????????????????????????????????????
            move = true;
        }
    }
}
