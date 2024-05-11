package com.sioptik.main

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sioptik.main.riwayat_repository.RiwayatEntity

class RiwayatRecyclerViewAdapter(
    private var riwayatList: List<RiwayatEntity>,
    private val interactionListener: RiwayatInteractionListener
) :
    RecyclerView.Adapter<RiwayatRecyclerViewAdapter.ModelViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RiwayatRecyclerViewAdapter.ModelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context).inflate(R.layout.riwayat_card, parent, false)
        return ModelViewHolder(layoutInflater)
    }

    override fun onBindViewHolder(holder: RiwayatRecyclerViewAdapter.ModelViewHolder, position: Int) {
        val riwayat = riwayatList[position]
        holder.bind(riwayat)
    }

    override fun getItemCount(): Int {
        return riwayatList.count()
    }

    fun updateData(newRiwayatList: List<RiwayatEntity>) {
        val diffCallback = RiwayatDiffCallback(riwayatList, newRiwayatList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        riwayatList = newRiwayatList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var cardView: CardView
        private var dateTextView: TextView
        private var apriltagIdTextView: TextView
        private var timeTextView: TextView
        private var hapusText: TextView
        private var lihatDetailButton: Button
        private var lihatGambarButton: Button

        init {
            cardView = itemView.findViewById(R.id.riwayat_card)
            dateTextView = itemView.findViewById(R.id.date_textview)
            apriltagIdTextView = itemView.findViewById(R.id.apriltag_id_textview)
            timeTextView = itemView.findViewById(R.id.time_textview)
            hapusText = itemView.findViewById(R.id.hapus_text)
            lihatDetailButton = itemView.findViewById(R.id.lihat_detail_button)
            lihatGambarButton = itemView.findViewById(R.id.lihat_gambar_button)

            hapusText.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val riwayat = riwayatList[position]
                    interactionListener.onDeleteRiwayat(riwayat)
                }
            }

            lihatDetailButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val riwayat = riwayatList[position]
                    interactionListener.onClickLihatDetail(riwayat)
                }
            }

            lihatGambarButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val riwayat = riwayatList[position]
                    interactionListener.onClickLihatGambar(riwayat)
                }
            }
        }

        fun bind(riwayat: RiwayatEntity) {
            val date = riwayat.date
            val dateText = "${date.day}/${date.month}/${date.year}"
            val apriltagIdText = "Apriltag ID: ${riwayat.apriltagId}"
            val timeText = "Taken at ${String.format("%02d", date.hours)}:${String.format("%02d", date.minutes)}"
            dateTextView.text = dateText
            apriltagIdTextView.text = apriltagIdText
            timeTextView.text = timeText
        }
    }
}

class RiwayatDiffCallback(
    private val oldList: List<RiwayatEntity>,
    private val newList: List<RiwayatEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return (
            oldList[oldItemPosition].id == newList[newItemPosition].id &&
            oldList[oldItemPosition].annotatedImageUri == newList[newItemPosition].annotatedImageUri &&
            oldList[oldItemPosition].originalImageUri == newList[newItemPosition].originalImageUri &&
            oldList[oldItemPosition].jsonFileUri == newList[newItemPosition].jsonFileUri &&
            oldList[oldItemPosition].date == newList[newItemPosition].date
        )
    }
}

