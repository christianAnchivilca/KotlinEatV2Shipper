package com.christian.kotlineatv2shipper.ui.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.christian.kotlineatv2shipper.R
import com.christian.kotlineatv2shipper.adapter.MyShippingOrderAdapter
import com.christian.kotlineatv2shipper.common.Common

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    var layoutAnimationController :LayoutAnimationController?=null
    var recycler_order:RecyclerView?=null
    var adapter:MyShippingOrderAdapter?=null

    @SuppressLint("FragmentLiveDataObserve", "UseRequireInsteadOfGet")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
         initView(root)
        homeViewModel.messageError.observe(this, Observer {
            Toast.makeText(context,""+it,Toast.LENGTH_LONG).show()
        })
        homeViewModel.getOrderMutableLiveData(Common.currentShipperUser!!.phone!!).observe(this, Observer {
              adapter = MyShippingOrderAdapter(context!!,it)
              recycler_order!!.adapter = adapter
              recycler_order!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initView(root: View?) {
        recycler_order = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recycler_order!!.setHasFixedSize(true)
        recycler_order!!.layoutManager = LinearLayoutManager(context)
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

    }
}