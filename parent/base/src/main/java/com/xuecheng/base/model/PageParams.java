package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

/**
 * 分页查询参数
 */
@Data
@ToString
public class PageParams {
    @ApiModelProperty("页码")
    private Long pageNo=1L;

    @ApiModelProperty("页面数")
    private Long pageSize=10L;

    public PageParams(Long pageNo, Long pageSize) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    public PageParams() {
    }
}
