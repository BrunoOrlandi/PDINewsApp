package com.example.pdinewsapp.ui.fragments

import android.content.ClipData.Item
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdinewsapp.R
import com.example.pdinewsapp.adapters.NewsAdapter
import com.example.pdinewsapp.databinding.FragmentFavoritesBinding
import com.example.pdinewsapp.ui.MainActivity
import com.example.pdinewsapp.ui.NewsViewModel
import com.google.android.material.snackbar.Snackbar

class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    lateinit var newsViewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    lateinit var binding: FragmentFavoritesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFavoritesBinding.bind(view)

        newsViewModel = (activity as MainActivity).newsViewModel
        setupFavoritesRecycler()

        newsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(R.id.action_favoritesFragment_to_articleFragment, bundle)
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val article = newsAdapter.differ.currentList[position]
                newsViewModel.deleteArticle(article)
                Snackbar.make(view, "Removed from favorites", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo"){
                        newsViewModel.addToFavorites(article)
                    }
                    show()
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).apply {
            attachToRecyclerView(binding.recyclerFavorites)
        }

        newsViewModel.getFavoriteNews().observe(viewLifecycleOwner, Observer { articles ->
            newsAdapter.differ.submitList(articles)
        })

    }

    private fun setupFavoritesRecycler() {
        newsAdapter = NewsAdapter()
        binding.recyclerFavorites.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

}