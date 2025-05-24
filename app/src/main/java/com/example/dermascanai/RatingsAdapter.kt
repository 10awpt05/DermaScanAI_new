    package com.example.dermascanai

    import android.icu.text.DateFormat
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import androidx.recyclerview.widget.RecyclerView
    import com.example.dermascanai.databinding.ItemReviewViewBinding
    import java.util.Date

    class RatingsAdapter(private val list: List<RatingModel>) :
        RecyclerView.Adapter<RatingsAdapter.RatingViewHolder>() {

        inner class RatingViewHolder(val binding: ItemReviewViewBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
            val binding = ItemReviewViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RatingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
            val feedback = list[position]
            with(holder.binding) {
                nameText.text = feedback.userName
                ratingBar.rating = feedback.rating
                ratingText.text = feedback.message
                dateText.text = DateFormat.getDateInstance().format(Date(feedback.timestamp))
            }
        }


        override fun getItemCount(): Int = list.size
    }
