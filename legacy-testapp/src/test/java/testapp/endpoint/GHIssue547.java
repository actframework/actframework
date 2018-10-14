package testapp.endpoint;

import org.junit.Test;

public class GHIssue547 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/547").header("Content-Type", "application/www-form-urlencoded; charset=UTF-8")
                .post().params(
                "table[draw]", 1,
                "table[columns][0][data]", "name",
                "table[columns][0][name]", "name",
                "table[columns][0][searchable]", true,
                "table[columns][0][orderable]", true,
                "table[columns][0][search][value]", "",
                "table[columns][0][search][regex]", false,
                "table[columns][1][data]", "vip",
                "table[columns][1][name]", "is_vip",
                "table[columns][1][searchable]", true,
                "table[columns][1][orderable]", false,
                "table[columns][1][search][value]", "",
                "table[columns][1][search][regex]", false,
                "table[columns][2][data]", "vipStartTime",
                "table[columns][2][name]", "vipStartTime",
                "table[columns][2][searchable]", true,
                "table[columns][2][orderable]", false,
                "table[columns][2][search][value]", "",
                "table[columns][2][search][regex]", false,
                "table[columns][3][data]", "vipEndTime",
                "table[columns][3][name]", "vipEndTime",
                "table[columns][3][searchable]", true,
                "table[columns][3][orderable]", false,
                "table[columns][3][search][value]", "",
                "table[columns][3][search][regex]", false,
                "table[columns][4][data]", "lastGoodsName",
                "table[columns][4][name]", "lastGoodsName",
                "table[columns][4][searchable]", true,
                "table[columns][4][orderable]", false,
                "table[columns][4][search][value]", "",
                "table[columns][4][search][regex]", false,
                "table[columns][5][data]", "regTime",
                "table[columns][5][name]", "regTime",
                "table[columns][5][searchable]", true,
                "table[columns][5][orderable]", true,
                "table[columns][5][search][value]", "",
                "table[columns][5][search][regex]", false,
                "table[columns][6][data]", "id",
                "table[columns][6][name]", "id",
                "table[columns][6][searchable]", true,
                "table[columns][6][orderable]", false,
                "table[columns][6][search][value]", "",
                "table[columns][6][search][regex]", false,
                "table[order][0][column]", 0,
                "table[order][0][dir]", "asc",
                "table[start]", 0,
                "table[length]", 30,
                "table[search][value]", "",
                "table[search][regex]", false
        );
        bodyEq("{\"columns\":[{\"data\":\"name\",\"name\":\"name\",\"orderable\":true,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"vip\",\"name\":\"is_vip\",\"orderable\":false,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"vipStartTime\",\"name\":\"vipStartTime\",\"orderable\":false,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"vipEndTime\",\"name\":\"vipEndTime\",\"orderable\":false,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"lastGoodsName\",\"name\":\"lastGoodsName\",\"orderable\":false,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"regTime\",\"name\":\"regTime\",\"orderable\":true,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true},{\"data\":\"id\",\"name\":\"id\",\"orderable\":false,\"search\":{\"regex\":false,\"value\":\"\"},\"searchable\":true}],\"data\":[],\"draw\":1,\"length\":30,\"order\":[{\"column\":0,\"dir\":\"asc\"}],\"orderProperty\":{\"column\":0,\"dir\":\"asc\"},\"recordsFiltered\":0,\"recordsTotal\":0,\"search\":{\"value\":\"\",\"regex\":\"false\"},\"start\":0}");
    }

}

