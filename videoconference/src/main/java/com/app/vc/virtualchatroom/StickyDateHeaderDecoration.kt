package com.app.vc.virtualchatroom

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
class StickyDateHeaderDecoration(
    private val adapter: VirtualChatMessageAdapter
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val child = parent.getChildAt(0) ?: return
        val position = parent.getChildAdapterPosition(child)

        if (position == RecyclerView.NO_POSITION) return
        if (isHeader(position) && child.top >= 0) return

        val headerPosition = getHeaderPositionForItem(position)
        if (headerPosition == -1) return

        val headerView = getHeaderView(parent, headerPosition)
        fixLayoutSize(parent, headerView)

        val contactPoint = headerView.bottom
        val childInContact = getChildInContact(parent, contactPoint)

        if (childInContact != null) {
            val childPos = parent.getChildAdapterPosition(childInContact)
            if (isHeader(childPos)) {
                moveHeader(c, headerView, childInContact)
                return
            }
        }

        drawHeader(c, headerView)
    }

    private fun getHeaderPositionForItem(itemPosition: Int): Int {
        var position = itemPosition

        while (position >= 0) {
            if (isHeader(position)) return position
            position--
        }
        return -1
    }

    private fun isHeader(position: Int): Boolean {
        return adapter.getItemViewType(position) == 5
    }

    private fun getHeaderView(parent: RecyclerView, position: Int): View {
        val holder = adapter.onCreateViewHolder(parent, 5)
        adapter.onBindViewHolder(holder, position)
        return holder.itemView
    }

    private fun drawHeader(canvas: Canvas, header: View) {
        canvas.save()
        canvas.translate(0f, 0f)
        header.draw(canvas)
        canvas.restore()
    }

    private fun moveHeader(canvas: Canvas, header: View, nextHeader: View) {
        canvas.save()
        canvas.translate(0f, (nextHeader.top - header.height).toFloat())
        header.draw(canvas)
        canvas.restore()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        for (i in 0 until parent.childCount) {

            val child = parent.getChildAt(i)

            if (child.bottom > contactPoint && child.top <= contactPoint) {
                return child
            }
        }
        return null
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            parent.width,
            View.MeasureSpec.EXACTLY
        )

        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            parent.height,
            View.MeasureSpec.UNSPECIFIED
        )

        view.measure(widthSpec, heightSpec)

        view.layout(
            0,
            0,
            view.measuredWidth,
            view.measuredHeight
        )
    }
}