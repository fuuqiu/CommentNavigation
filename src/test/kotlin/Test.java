import java.util.List;
import java.util.Map;

public interface Test {
    /**
     * 新增
     */
    int insert(String reserveRecord);

    /**
     * 删除
     * @return
     */
    int delete(Long id);

    //更新
    int update(String reserveRecord);

    //根据主键 id 查询
    String selectPrimaryKey(Long id);

    /**
     * 查询预约信息
     * @return
     */
    List<Map<String,Object>> findReserveList(String str);
}
