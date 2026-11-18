package com.xh.hotme.bean;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.List;

/**
 * {
 * "beginTime": "2024-01-03T09:51:28.427Z",
 * "competitionBeginTime": "2024-01-03T09:51:28.427Z",
 * "competitionEndTime": "2024-01-03T09:51:28.427Z",
 * "competitionName": "string",
 * "competitionStatus": 0,
 * "competitionTime": "2024-01-03T09:51:28.427Z",
 * "deviceId": "string",
 * "endTime": "2024-01-03T09:51:28.427Z",
 * "motionType": "string",
 * "pageNum": 0,
 * "pageSize": 0
 * }
 * }
 */
@Keep
public class CompetitionListResultBean implements Serializable {
    private Integer endRow;
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
    private Boolean isFirstPage;
    private Boolean isLastPage;
    private List<HmCompetition> list;
    private Integer navigateFirstPage;
    private Integer navigateLastPage;
    private Integer navigatePages;
    private List<Integer> navigatepageNums;
    private Integer nextPage;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;
    private Integer prePage;
    private Integer size;
    private Integer startRow;
    private Integer total;


    public Integer getEndRow() {
        return endRow;
    }

    public void setEndRow(Integer endRow) {
        this.endRow = endRow;
    }

    public Boolean getHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(Boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public Boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(Boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    public Boolean getFirstPage() {
        return isFirstPage;
    }

    public void setFirstPage(Boolean firstPage) {
        isFirstPage = firstPage;
    }

    public Boolean getLastPage() {
        return isLastPage;
    }

    public void setLastPage(Boolean lastPage) {
        isLastPage = lastPage;
    }

    public List<HmCompetition> getList() {
        return list;
    }

    public void setList(List<HmCompetition> list) {
        this.list = list;
    }

    public Integer getNavigateFirstPage() {
        return navigateFirstPage;
    }

    public void setNavigateFirstPage(Integer navigateFirstPage) {
        this.navigateFirstPage = navigateFirstPage;
    }

    public Integer getNavigateLastPage() {
        return navigateLastPage;
    }

    public void setNavigateLastPage(Integer navigateLastPage) {
        this.navigateLastPage = navigateLastPage;
    }

    public Integer getNavigatePages() {
        return navigatePages;
    }

    public void setNavigatePages(Integer navigatePages) {
        this.navigatePages = navigatePages;
    }

    public List<Integer> getNavigatepageNums() {
        return navigatepageNums;
    }

    public void setNavigatepageNums(List<Integer> navigatepageNums) {
        this.navigatepageNums = navigatepageNums;
    }

    public Integer getNextPage() {
        return nextPage;
    }

    public void setNextPage(Integer nextPage) {
        this.nextPage = nextPage;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getPrePage() {
        return prePage;
    }

    public void setPrePage(Integer prePage) {
        this.prePage = prePage;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
